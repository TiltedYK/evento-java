package service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory reaction store (session-level).
 * Supports 4 reaction types: LIKE, LOVE, WOW, FIRE.
 * Key format: type + ":" + itemId  (e.g. "post:5", "news:2")
 */
public class ReactionService {

    public enum Type { LIKE, LOVE, WOW, FIRE;
        public String emoji() {
            return switch (this) { case LIKE -> "👍"; case LOVE -> "❤"; case WOW -> "😮"; case FIRE -> "🔥"; };
        }
    }

    // store[key][userId] = reactionType
    private static final Map<String, Map<Integer, Type>> store = new HashMap<>();

    public static void react(String key, int userId, Type reaction) {
        Map<Integer, Type> m = store.computeIfAbsent(key, k -> new HashMap<>());
        if (reaction.equals(m.get(userId))) {
            m.remove(userId); // toggle off
        } else {
            m.put(userId, reaction);
        }
    }

    public static Type getUserReaction(String key, int userId) {
        Map<Integer, Type> m = store.get(key);
        return m != null ? m.get(userId) : null;
    }

    public static Map<Type, Long> getCounts(String key) {
        Map<Integer, Type> m = store.getOrDefault(key, Collections.emptyMap());
        return Arrays.stream(Type.values())
                .collect(Collectors.toMap(t -> t,
                        t -> m.values().stream().filter(v -> v == t).count()));
    }

    public static long totalReactions(String key) {
        return store.getOrDefault(key, Collections.emptyMap()).size();
    }

    /** Pre-seed some reactions so the feed looks alive on first load */
    public static void seed(String key, long likeCount, long loveCount, long wowCount, long fireCount) {
        if (store.containsKey(key)) return;
        Map<Integer, Type> m = store.computeIfAbsent(key, k -> new HashMap<>());
        int uid = -1;
        for (long i = 0; i < likeCount; i++) m.put(uid--, Type.LIKE);
        for (long i = 0; i < loveCount; i++) m.put(uid--, Type.LOVE);
        for (long i = 0; i < wowCount;  i++) m.put(uid--, Type.WOW);
        for (long i = 0; i < fireCount; i++) m.put(uid--, Type.FIRE);
    }
}
