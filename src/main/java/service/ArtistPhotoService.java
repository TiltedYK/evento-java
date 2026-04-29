package service;

import java.util.List;

/**
 * Resolves a real portrait / artwork URL for an artist name.
 * <ol>
 *   <li>iTunes {@code musicArtist} catalog (official promo art)</li>
 *   <li>First matching track's album artwork</li>
 *   <li>Wikipedia article thumbnail (often a press photo)</li>
 * </ol>
 * Returns {@code null} if nothing is found — callers should show a local music motif.
 *
 * Build tag: {@code EVENTO_ARTIST_PHOTO_BUILD_2026_04_29_ITUNES_WIKI_V1}
 */
public final class ArtistPhotoService {

    public static final String BUILD_TAG = "EVENTO_ARTIST_PHOTO_BUILD_2026_04_29_ITUNES_WIKI_V1";

    private ArtistPhotoService() {}

    public static String resolveArtistImageUrl(String artistName) {
        if (artistName == null || artistName.isBlank()) return null;

        String a = ItunesService.artistArtworkUrl(artistName);
        if (isHttp(a)) return a;

        List<ItunesService.Track> tracks = ItunesService.searchArtist(artistName, 3);
        for (ItunesService.Track t : tracks) {
            String art = t.artworkUrl();
            if (art != null && !art.isBlank() && art.startsWith("http")) {
                return art.replace("/100x100bb", "/600x600bb")
                        .replace("/60x60bb", "/600x600bb")
                        .replace("/30x30bb", "/600x600bb");
            }
        }

        String wiki = WikipediaService.getThumbnailUrl(artistName);
        if (isHttp(wiki)) return wiki;

        return null;
    }

    private static boolean isHttp(String u) {
        return u != null && !u.isBlank() && (u.startsWith("http://") || u.startsWith("https://"));
    }
}
