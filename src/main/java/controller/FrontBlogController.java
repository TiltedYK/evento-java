package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.Comment;
import model.Post;
import model.User;
import service.*;
import util.OverlayService;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BLOG/FEED module — Facebook-style scrollable feed.
 *
 * APIs:
 *   • MusicNewsService → Dev.to API  (live music articles in right sidebar + mixed into feed)
 *   • QuoteService     → quotable.io (daily inspiration in sidebar)
 *
 * Advanced features:
 *   1. Reactions (👍 Like / ❤ Love / 😮 Wow / 🔥 Fire) — toggle per user, live count
 *   2. Inline comment form — post comments without leaving the feed
 *   3. Reading time estimator on each card
 */
public class FrontBlogController implements Initializable {

    @FXML private VBox              feedList;
    @FXML private VBox              newsSidebar;
    @FXML private VBox              createPostBox;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  filterCombo;
    @FXML private Label             countLabel;
    @FXML private Label             newsStatusLabel;
    @FXML private Label             quoteText;
    @FXML private Label             quoteAuthor;

    private final PostService    postService    = new PostService();
    private final CommentService commentService = new CommentService();

    private List<Post>                    localPosts;
    private List<MusicNewsService.Article> newsArticles;
    private User currentUser;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Exposed so showNewsArticle can pre-fill them
    private javafx.scene.control.TextField postTitleInput;
    private javafx.scene.control.TextArea  postBodyInput;
    private VBox postExpandedBox;
    private HBox postCollapsedBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCombo.getItems().addAll("All Posts", "Local Posts", "Music News", "Most Reactions");
        filterCombo.setValue("All Posts");
        buildCreateBox();
        loadDataAsync();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Rebuild post-creation box and feed now that we have the logged-in user
        Platform.runLater(() -> {
            buildCreateBox();
            if (localPosts != null) buildFeed();  // re-render so comment forms appear
        });
    }

    // ── Post creation box ─────────────────────────────────────────────

    private void buildCreateBox() {
        if (createPostBox == null) return;
        createPostBox.getChildren().clear();
        createPostBox.setStyle("-fx-padding:16 28 16 28;");

        // Collapsed state
        postCollapsedBox = new HBox(12);
        postCollapsedBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label avatar = new Label("✏️"); avatar.setStyle("-fx-font-size:20px;");
        javafx.scene.control.TextField prompt = new javafx.scene.control.TextField();
        prompt.setPromptText("What's on your mind? Share a post, or click a news article to draft one…");
        prompt.getStyleClass().add("post-create-input");
        HBox.setHgrow(prompt, Priority.ALWAYS);
        postCollapsedBox.getChildren().addAll(avatar, prompt);

        // Expanded state
        postExpandedBox = new VBox(10);
        postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
        postTitleInput = new javafx.scene.control.TextField();
        postTitleInput.setPromptText("Post title…");
        postTitleInput.getStyleClass().add("post-create-input");
        postBodyInput = new javafx.scene.control.TextArea();
        postBodyInput.setPromptText("Write your post here…");
        postBodyInput.setPrefRowCount(4); postBodyInput.setWrapText(true);
        postBodyInput.getStyleClass().add("overlay-textarea");
        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("card-cancel-btn");
        Button postBtn   = new Button("📢  SHARE POST"); postBtn.getStyleClass().add("post-create-btn");
        actions.getChildren().addAll(cancelBtn, postBtn);
        postExpandedBox.getChildren().addAll(postTitleInput, postBodyInput, actions);

        prompt.setOnMouseClicked(e -> expandCreateBox(null, null));

        cancelBtn.setOnAction(e -> {
            postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
            postCollapsedBox.setVisible(true);  postCollapsedBox.setManaged(true);
            postTitleInput.clear(); postBodyInput.clear();
        });

        postBtn.setOnAction(e -> {
            String title = postTitleInput.getText().trim();
            String body  = postBodyInput.getText().trim();
            if (title.isEmpty()) { postTitleInput.setStyle(postTitleInput.getStyle() + ";-fx-border-color:#E8320A;"); return; }
            int authorId = currentUser != null ? currentUser.getId() : 1;
            try {
                Post post = new Post(authorId, title, body);
                postService.ajouter(post);
                postTitleInput.clear(); postBodyInput.clear();
                postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
                postCollapsedBox.setVisible(true);  postCollapsedBox.setManaged(true);
                try { localPosts = postService.recuperer(); } catch (Exception ignored) {}
                buildFeed();
            } catch (java.sql.SQLException ex) { ex.printStackTrace(); }
        });

        createPostBox.getChildren().addAll(postCollapsedBox, postExpandedBox);
    }

    /** Open/expand the create-post box, optionally pre-filling title and body. */
    private void expandCreateBox(String title, String body) {
        if (postCollapsedBox != null) { postCollapsedBox.setVisible(false); postCollapsedBox.setManaged(false); }
        if (postExpandedBox  != null) { postExpandedBox.setVisible(true);   postExpandedBox.setManaged(true);  }
        if (postTitleInput != null) {
            if (title != null) postTitleInput.setText(title);
            postTitleInput.requestFocus();
        }
        if (postBodyInput != null && body != null) postBodyInput.setText(body);
    }

    // ── Load data ─────────────────────────────────────────────────────

    private void loadDataAsync() {
        new Thread(() -> {
            // 1. Local posts
            try { localPosts = postService.recuperer(); } catch (Exception e) { localPosts = new ArrayList<>(); }

            // Seed some demo reactions so the feed looks alive
            for (int i = 0; i < localPosts.size(); i++) {
                int id = localPosts.get(i).getId();
                ReactionService.seed("post:" + id, (long)(Math.random()*30+5),
                        (long)(Math.random()*15+2), (long)(Math.random()*8),
                        (long)(Math.random()*12+1));
            }

            // 2. Music news
            Platform.runLater(() -> newsStatusLabel.setText("Fetching music news from Dev.to…"));
            newsArticles = MusicNewsService.getLatestNews();
            for (int i = 0; i < newsArticles.size(); i++) {
                ReactionService.seed("news:" + i,
                        newsArticles.get(i).reactions() / 3,
                        newsArticles.get(i).reactions() / 5,
                        newsArticles.get(i).reactions() / 8,
                        newsArticles.get(i).reactions() / 4);
            }

            // 3. Quote
            String[] quote = QuoteService.getRandomQuote();

            Platform.runLater(() -> {
                newsStatusLabel.setText(newsArticles.size() + " articles loaded");
                quoteText.setText("\"" + quote[0] + "\"");
                quoteAuthor.setText("— " + quote[1]);
                buildNewsSidebar();
                buildFeed();
            });
        }, "blog-loader").start();
    }

    // ── News sidebar ──────────────────────────────────────────────────

    private void buildNewsSidebar() {
        newsSidebar.getChildren().clear();
        if (newsArticles == null) return;
        newsArticles.forEach(a -> {
            VBox item = new VBox(4);
            item.getStyleClass().add("news-sidebar-item");
            item.setOnMouseClicked(e -> showNewsArticle(a, newsArticles.indexOf(a)));

            Label title = new Label(a.title().length() > 60 ? a.title().substring(0,60)+"…" : a.title());
            title.getStyleClass().add("news-sidebar-title");
            title.setWrapText(true);
            Label meta = new Label(a.author() + " · " + a.publishedAt());
            meta.getStyleClass().add("news-sidebar-meta");
            Label reactions = new Label("👍 " + a.reactions() + "  💬 " + a.comments());
            reactions.getStyleClass().add("news-sidebar-stats");
            item.getChildren().addAll(title, meta, reactions);
            newsSidebar.getChildren().add(item);
        });
    }

    // ── Feed ──────────────────────────────────────────────────────────

    @FXML void filterFeed() { buildFeed(); }

    private void buildFeed() {
        feedList.getChildren().clear();
        String q    = searchField.getText().toLowerCase().trim();
        String mode = filterCombo.getValue();

        List<FeedItem> items = new ArrayList<>();

        // Local posts
        if (!"Music News".equals(mode) && localPosts != null) {
            localPosts.stream()
                    .filter(p -> q.isEmpty()
                            || (p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                            || (p.getContent() != null && p.getContent().toLowerCase().contains(q)))
                    .forEach(p -> items.add(new FeedItem(p, null, -1)));
        }

        // News articles
        if (!"Local Posts".equals(mode) && newsArticles != null) {
            newsArticles.stream()
                    .filter(a -> q.isEmpty() || a.title().toLowerCase().contains(q)
                            || a.description().toLowerCase().contains(q))
                    .forEach(a -> items.add(new FeedItem(null, a, newsArticles.indexOf(a))));
        }

        // Sort
        if ("Most Reactions".equals(mode)) {
            items.sort((a, b) -> Long.compare(
                    ReactionService.totalReactions(b.key()),
                    ReactionService.totalReactions(a.key())));
        } else {
            items.sort((a, b) -> {
                if (a.post != null && b.post != null)
                    return Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                            .compare(a.post, b.post);
                if (a.article != null && b.article == null) return -1;
                if (a.post    != null && b.article != null) return 1;
                return 0;
            });
        }

        countLabel.setText(items.size() + " post" + (items.size() == 1 ? "" : "s"));

        for (int i = 0; i < items.size(); i++) {
            VBox card = items.get(i).isNews() ? buildNewsCard(items.get(i)) : buildPostCard(items.get(i));
            feedList.getChildren().add(card);
            card.setOpacity(0);
            final int delay = i * 40;
            PauseTransition p = new PauseTransition(Duration.millis(delay));
            p.setOnFinished(ev -> { FadeTransition ft = new FadeTransition(Duration.millis(250), card); ft.setToValue(1); ft.play(); });
            p.play();
        }

        if (items.isEmpty()) {
            Label empty = new Label("📭  Nothing here yet.");
            empty.getStyleClass().add("front-empty-label");
            empty.setStyle("-fx-padding:40 0 0 32;");
            feedList.getChildren().add(empty);
        }
    }

    // ── Local post card ───────────────────────────────────────────────

    private VBox buildPostCard(FeedItem item) {
        Post post = item.post;
        VBox card = new VBox(0);
        card.getStyleClass().add("feed-card");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("feed-card-header");
        Label avatar = new Label("👤"); avatar.getStyleClass().add("feed-avatar");
        VBox authorInfo = new VBox(1);
        Label authorName = new Label("EVENTO User #" + post.getAuthorId()); authorName.getStyleClass().add("feed-author");
        String dateStr = post.getCreatedAt() != null ? post.getCreatedAt().format(DF) : "";
        Label dateLabel = new Label(dateStr); dateLabel.getStyleClass().add("feed-date");
        authorInfo.getChildren().addAll(authorName, dateLabel);
        int words = wordCount(post.getContent()); int mins = Math.max(1, words / 200);
        Label rt = new Label("⏱ " + mins + " min read"); rt.getStyleClass().add("feed-read-time");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(avatar, authorInfo, sp, rt);

        // Body
        VBox body = new VBox(8);
        body.getStyleClass().add("feed-card-body");
        Label title = new Label(post.getTitle() != null ? post.getTitle() : "Untitled");
        title.getStyleClass().add("feed-post-title"); title.setWrapText(true);
        String preview = post.getContent() != null
                ? (post.getContent().length() > 220 ? post.getContent().substring(0, 220) + "…" : post.getContent())
                : "";
        Label content = new Label(preview); content.getStyleClass().add("feed-post-content"); content.setWrapText(true);
        body.getChildren().addAll(title, content);

        // Reactions + comments footer
        String key = "post:" + post.getId();
        VBox footer = buildFeedFooter(key, post.getId(), true);

        card.getChildren().addAll(header, body, footer);
        return card;
    }

    // ── News article card ─────────────────────────────────────────────

    private VBox buildNewsCard(FeedItem item) {
        MusicNewsService.Article a = item.article;
        int idx = item.newsIdx;
        VBox card = new VBox(0);
        card.getStyleClass().addAll("feed-card", "feed-card-news");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("feed-card-header");
        Label icon = new Label("📡"); icon.getStyleClass().add("feed-avatar");
        VBox authorInfo = new VBox(1);
        Label src  = new Label(a.author());  src.getStyleClass().add("feed-author");
        Label date = new Label(a.publishedAt() + " · Dev.to"); date.getStyleClass().add("feed-date");
        authorInfo.getChildren().addAll(src, date);
        Label newsBadge = new Label("🔴 LIVE NEWS"); newsBadge.getStyleClass().add("feed-live-badge");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(icon, authorInfo, sp, newsBadge);

        // Body
        VBox body = new VBox(8);
        body.getStyleClass().add("feed-card-body");
        Label title   = new Label(a.title()); title.getStyleClass().add("feed-post-title"); title.setWrapText(true);
        Label desc    = new Label(a.description()); desc.getStyleClass().add("feed-post-content"); desc.setWrapText(true);
        Button readMore = new Button("Read on Dev.to →"); readMore.getStyleClass().add("feed-read-more-btn");
        readMore.setOnAction(e -> showNewsArticle(a, idx));
        body.getChildren().addAll(title, desc, readMore);

        // Reactions
        String key = "news:" + idx;
        VBox footer = buildFeedFooter(key, idx, false);

        card.getChildren().addAll(header, body, footer);
        return card;
    }

    // ── Reactions + Comments footer ────────────────────────────────────

    private VBox buildFeedFooter(String key, int id, boolean isPost) {
        VBox footer = new VBox(0);
        footer.getStyleClass().add("feed-footer");

        // Reaction bar
        HBox reactionBar = new HBox(4);
        reactionBar.getStyleClass().add("feed-reaction-bar");
        reactionBar.setAlignment(Pos.CENTER_LEFT);

        for (ReactionService.Type type : ReactionService.Type.values()) {
            VBox btn = buildReactionBtn(key, type);
            reactionBar.getChildren().add(btn);
        }

        Region rsp = new Region(); HBox.setHgrow(rsp, Priority.ALWAYS);

        // Comment toggle button
        Button commentBtn = new Button("💬  Comments");
        commentBtn.getStyleClass().add("feed-comment-toggle");

        // Comment section (initially hidden)
        VBox commentSection = new VBox(8);
        commentSection.getStyleClass().add("feed-comment-section");
        commentSection.setVisible(false); commentSection.setManaged(false);
        if (isPost) buildCommentSection(commentSection, id);

        commentBtn.setOnAction(e -> {
            boolean open = !commentSection.isVisible();
            commentSection.setVisible(open); commentSection.setManaged(open);
            commentBtn.setText(open ? "💬  Hide Comments" : "💬  Comments");
        });

        HBox footerRow = new HBox(10, reactionBar, rsp, commentBtn);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().addAll(footerRow, commentSection);
        return footer;
    }

    private VBox buildReactionBtn(String key, ReactionService.Type type) {
        long count = ReactionService.getCounts(key).getOrDefault(type, 0L);
        Label lbl = new Label(type.emoji() + "  " + count);
        lbl.getStyleClass().add("reaction-btn");

        // Use the real userId so reactions survive feed rebuilds
        int userId = currentUser != null ? currentUser.getId() : Integer.MIN_VALUE;
        if (userId != Integer.MIN_VALUE && type.equals(ReactionService.getUserReaction(key, userId)))
            lbl.getStyleClass().add("reaction-btn-active");

        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setOnMouseClicked(e -> {
            if (userId == Integer.MIN_VALUE) return; // guest — can't react
            ReactionService.react(key, userId, type);
            long newCount = ReactionService.getCounts(key).getOrDefault(type, 0L);
            lbl.setText(type.emoji() + "  " + newCount);
            boolean active = type.equals(ReactionService.getUserReaction(key, userId));
            if (active) { if (!lbl.getStyleClass().contains("reaction-btn-active")) lbl.getStyleClass().add("reaction-btn-active"); }
            else lbl.getStyleClass().remove("reaction-btn-active");

            // Bounce animation
            ScaleTransition bounce = new ScaleTransition(Duration.millis(120), lbl);
            bounce.setToX(1.3); bounce.setToY(1.3); bounce.setCycleCount(2); bounce.setAutoReverse(true);
            bounce.play();
        });
        return box;
    }

    private void buildCommentSection(VBox section, int postId) {
        try {
            List<Comment> comments = commentService.recuperer().stream()
                    .filter(c -> c.getPostId() == postId)
                    .sorted(Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5).collect(Collectors.toList());

            if (!comments.isEmpty()) {
                VBox list = new VBox(6);
                comments.forEach(c -> {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.TOP_LEFT);
                    Label av = new Label("👤"); av.getStyleClass().add("feed-comment-avatar");
                    VBox cb = new VBox(2);
                    Label ct = new Label(c.getContent()); ct.getStyleClass().add("feed-comment-text"); ct.setWrapText(true);
                    String cd = c.getCreatedAt() != null ? c.getCreatedAt().format(DF) : "";
                    Label dt = new Label(cd); dt.getStyleClass().add("feed-comment-date");
                    cb.getChildren().addAll(ct, dt); HBox.setHgrow(cb, Priority.ALWAYS);
                    row.getChildren().addAll(av, cb);
                    list.getChildren().add(row);
                });
                section.getChildren().add(list);
            }
        } catch (Exception ignored) {}

        if (currentUser != null) {
            HBox inputRow = new HBox(8);
            inputRow.setAlignment(Pos.CENTER_LEFT);
            inputRow.getStyleClass().add("feed-comment-input-row");
            javafx.scene.control.TextField input = new javafx.scene.control.TextField();
            input.setPromptText("Write a comment…");
            input.getStyleClass().add("feed-comment-input");
            HBox.setHgrow(input, Priority.ALWAYS);
            Button post = new Button("POST");
            post.getStyleClass().add("feed-comment-post-btn");
            post.setOnAction(e -> {
                String text = input.getText().trim();
                if (text.isEmpty()) return;
                try {
                    Comment c = new Comment(postId, currentUser.getId(), text);
                    commentService.ajouter(c);
                    input.clear();
                    section.getChildren().clear();
                    buildCommentSection(section, postId);
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
            inputRow.getChildren().addAll(input, post);
            section.getChildren().add(inputRow);
        }
    }

    private void showNewsArticle(MusicNewsService.Article a, int idx) {
        VBox content = new VBox(14);
        content.getStyleClass().add("overlay-body");

        Label src = new Label("📡 " + a.author() + " · " + a.publishedAt());
        src.getStyleClass().add("overlay-post-meta");

        Label title = new Label(a.title()); title.getStyleClass().add("overlay-post-title"); title.setWrapText(true);

        Label desc = new Label(a.description()); desc.getStyleClass().add("overlay-post-body"); desc.setWrapText(true);

        HBox stats = new HBox(16);
        Label r  = new Label("👍 " + a.reactions() + " reactions"); r.getStyleClass().add("feed-read-time");
        Label cm = new Label("💬 " + a.comments()  + " comments");  cm.getStyleClass().add("feed-read-time");
        stats.getChildren().addAll(r, cm);

        Label link = new Label("🔗 " + a.url()); link.getStyleClass().add("comment-date"); link.setWrapText(true);

        // Action buttons
        HBox actions = new HBox(10);
        Button copyUrl = new Button("📋  Copy URL");
        copyUrl.getStyleClass().add("card-action-btn-outline");
        copyUrl.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cb = new javafx.scene.input.ClipboardContent();
            cb.putString(a.url());
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cb);
            copyUrl.setText("✅  Copied!");
        });

        Button draftPost = new Button("📝  Write a Post About This");
        draftPost.getStyleClass().add("post-create-btn");
        draftPost.setOnAction(e -> {
            OverlayService.hide();
            // Pre-fill title with article title, body with description + link
            String preTitle = a.title();
            String preBody  = "📰 " + a.description()
                    + "\n\nSource: " + a.url();
            expandCreateBox(preTitle, preBody);
        });
        actions.getChildren().addAll(copyUrl, draftPost);

        content.getChildren().addAll(src, title, desc, stats, link, actions);
        OverlayService.show("Music News", content);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private int wordCount(String t) {
        return (t == null || t.isBlank()) ? 0 : t.trim().split("\\s+").length;
    }

    record FeedItem(Post post, MusicNewsService.Article article, int newsIdx) {
        boolean isNews() { return article != null; }
        String key() { return isNews() ? "news:" + newsIdx : "post:" + post.getId(); }
    }
}
