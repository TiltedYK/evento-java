package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Free no-key trivia from the <a href="https://opentdb.com/">Open Trivia DB</a>.
 * Music category id = 12. Used in the Blog sidebar to make the page feel less
 * static.
 *
 * Build tag: {@code EVENTO_TRIVIA_BUILD_2026_04_28_OPENTDB_V1}
 */
public final class MusicTriviaService {

    public static final String BUILD_TAG = "EVENTO_TRIVIA_BUILD_2026_04_28_OPENTDB_V1";

    private MusicTriviaService() {}

    public record TriviaQuestion(
            String question,
            String correctAnswer,
            List<String> shuffledAnswers,
            String difficulty) {}

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    /** Returns a multiple-choice music question, or a hand-curated fallback. */
    public static TriviaQuestion getMusicTrivia() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://opentdb.com/api.php?amount=1&category=12&type=multiple"))
                    .timeout(Duration.ofSeconds(6))
                    .header("Accept", "application/json").GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                TriviaQuestion q = parse(resp.body());
                if (q != null) return q;
            }
        } catch (Exception ignored) {}
        return fallback();
    }

    private static TriviaQuestion parse(String body) {
        String question = jsonString(body, "question");
        String correct  = jsonString(body, "correct_answer");
        String diff     = jsonString(body, "difficulty");
        if (question == null || correct == null) return null;

        List<String> incorrect = jsonStringArray(body, "incorrect_answers");
        List<String> all = new ArrayList<>(incorrect);
        all.add(correct);
        Collections.shuffle(all);

        return new TriviaQuestion(decode(question), decode(correct),
                all.stream().map(MusicTriviaService::decode).toList(),
                diff == null ? "medium" : diff);
    }

    private static String jsonString(String json, String key) {
        String s = "\"" + key + "\":\"";
        int idx = json.indexOf(s);
        if (idx < 0) return null;
        int start = idx + s.length();
        StringBuilder sb = new StringBuilder();
        for (int p = start; p < json.length(); p++) {
            char c = json.charAt(p);
            if (c == '\\' && p + 1 < json.length()) {
                sb.append(json.charAt(p + 1));
                p++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String> jsonStringArray(String json, String key) {
        List<String> out = new ArrayList<>();
        String s = "\"" + key + "\":[";
        int idx = json.indexOf(s);
        if (idx < 0) return out;
        int p = idx + s.length();
        while (p < json.length()) {
            while (p < json.length() && json.charAt(p) != '"' && json.charAt(p) != ']') p++;
            if (p >= json.length() || json.charAt(p) == ']') break;
            int start = p + 1;
            StringBuilder sb = new StringBuilder();
            for (p = start; p < json.length(); p++) {
                char c = json.charAt(p);
                if (c == '\\' && p + 1 < json.length()) { sb.append(json.charAt(p + 1)); p++; }
                else if (c == '"') break;
                else sb.append(c);
            }
            out.add(sb.toString());
            p++;
        }
        return out;
    }

    /** Decode HTML entities the OpenTDB API uses by default (encode=default). */
    private static String decode(String s) {
        if (s == null) return "";
        return s.replace("&quot;", "\"")
                .replace("&#039;", "'").replace("&apos;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replace("&eacute;", "é").replace("&Eacute;", "É")
                .replace("&aacute;", "á").replace("&iacute;", "í")
                .replace("&oacute;", "ó").replace("&uacute;", "ú")
                .replace("&ntilde;", "ñ");
    }

    private static TriviaQuestion fallback() {
        TriviaQuestion[] pool = new TriviaQuestion[]{
                new TriviaQuestion(
                        "Which Tunisian-born singer hit international fame with the song \"Ya Ghali\"?",
                        "Latifa",
                        List.of("Latifa", "Saber Rebai", "Lotfi Bouchnak", "Amina Annabi"),
                        "easy"),
                new TriviaQuestion(
                        "Which year did Linkin Park release \"Hybrid Theory\"?",
                        "2000",
                        List.of("1998", "2000", "2003", "2005"),
                        "medium"),
                new TriviaQuestion(
                        "Which K-Pop group does the song \"Uh-Oh\" belong to?",
                        "(G)I-DLE",
                        List.of("Twice", "(G)I-DLE", "BLACKPINK", "ITZY"),
                        "easy"),
                new TriviaQuestion(
                        "What instrument is most associated with Anouar Brahem?",
                        "Oud",
                        List.of("Oud", "Qanun", "Ney", "Violin"),
                        "easy")
        };
        return pool[(int) (Math.random() * pool.length)];
    }
}
