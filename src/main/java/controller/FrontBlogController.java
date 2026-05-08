package controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import model.Comment;
import model.Post;
import model.User;
import service.*;
import util.OverlayService;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.net.URL;
import java.util.ResourceBundle;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BLOG/FEED — scrollable feed of <b>database posts only</b> (no injected Dev.to cards).
 *
 * APIs:
 *   • MusicNewsService → Dev.to (sidebar list + full in-app browser)
 *   • QuoteService / MusicTriviaService → sidebar extras
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

    @FXML private VBox              triviaBox;
    @FXML private Label             triviaQuestion;
    @FXML private VBox              triviaAnswers;
    @FXML private Label             triviaResult;

    private final PostService    postService    = new PostService();
    private final CommentService commentService = new CommentService();
    private final UserService    userService    = new UserService();

    private List<Post>                    localPosts;
    private List<MusicNewsService.Article> newsArticles;
    private User currentUser;
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private javafx.scene.control.TextField postTitleInput;
    private javafx.scene.control.TextArea  postBodyInput;
    private VBox postExpandedBox;
    private HBox postCollapsedBox;
    /** Relative filename under {@code uploads/} for the next post, or null. */
    private String pendingPostImageFilename;
    private Label postImageHint;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCombo.getItems().addAll("All Posts", "Most Reactions");
        filterCombo.setValue("All Posts");
        buildCreateBox();
        loadDataAsync();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        Platform.runLater(() -> {
            buildCreateBox();
            if (localPosts != null) buildFeed();
        });
    }

    // ── Post creation box ─────────────────────────────────────────────

    private void buildCreateBox() {
        if (createPostBox == null) return;
        createPostBox.getChildren().clear();
        createPostBox.setStyle("-fx-padding:16 28 16 28;");

        postCollapsedBox = new HBox(12);
        postCollapsedBox.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("✏️"); avatar.setStyle("-fx-font-size:20px;");
        javafx.scene.control.TextField prompt = new javafx.scene.control.TextField();
        prompt.setPromptText("What's on your mind? Share something with the community…");
        prompt.getStyleClass().add("post-create-input");
        HBox.setHgrow(prompt, Priority.ALWAYS);
        postCollapsedBox.getChildren().addAll(avatar, prompt);

        postExpandedBox = new VBox(10);
        postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
        postTitleInput = new javafx.scene.control.TextField();
        postTitleInput.setPromptText("Post title…");
        postTitleInput.getStyleClass().add("post-create-input");
        postBodyInput = new javafx.scene.control.TextArea();
        postBodyInput.setPromptText("Write your post here…");
        postBodyInput.setPrefRowCount(4); postBodyInput.setWrapText(true);
        postBodyInput.getStyleClass().add("overlay-textarea");
        HBox imageRow = new HBox(10);
        imageRow.setAlignment(Pos.CENTER_LEFT);
        Button pickImg = new Button("📷  Photo (optional)");
        pickImg.getStyleClass().add("card-action-btn-outline");
        postImageHint = new Label("No image — cover will use title gradient only.");
        postImageHint.getStyleClass().add("card-meta-text");
        postImageHint.setWrapText(true);
        HBox.setHgrow(postImageHint, Priority.ALWAYS);
        pickImg.setOnAction(e -> pickPostCoverImage());
        imageRow.getChildren().addAll(pickImg, postImageHint);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button cancelBtn = new Button("Cancel"); cancelBtn.getStyleClass().add("card-cancel-btn");
        Button postBtn   = new Button("📢  SHARE POST"); postBtn.getStyleClass().add("post-create-btn");
        actions.getChildren().addAll(cancelBtn, postBtn);
        postExpandedBox.getChildren().addAll(postTitleInput, postBodyInput, imageRow, actions);

        prompt.setOnMouseClicked(e -> expandCreateBox(null, null));

        cancelBtn.setOnAction(e -> {
            postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
            postCollapsedBox.setVisible(true);  postCollapsedBox.setManaged(true);
            postTitleInput.clear(); postBodyInput.clear();
            pendingPostImageFilename = null;
            refreshPostImageHint();
        });

        postBtn.setOnAction(e -> {
            String title = postTitleInput.getText().trim();
            String body  = postBodyInput.getText().trim();
            if (title.isEmpty()) { postTitleInput.setStyle(postTitleInput.getStyle() + ";-fx-border-color:#E8320A;"); return; }
            int authorId = currentUser != null ? currentUser.getId() : 1;
            try {
                Post post = new Post(authorId, title, body);
                if (pendingPostImageFilename != null && !pendingPostImageFilename.isBlank())
                    post.setImage(pendingPostImageFilename);
                postService.ajouter(post);
                postTitleInput.clear(); postBodyInput.clear();
                pendingPostImageFilename = null;
                refreshPostImageHint();
                postExpandedBox.setVisible(false); postExpandedBox.setManaged(false);
                postCollapsedBox.setVisible(true);  postCollapsedBox.setManaged(true);
                try { localPosts = postService.recuperer(); } catch (Exception ignored) {}
                buildFeed();
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        createPostBox.getChildren().addAll(postCollapsedBox, postExpandedBox);
    }

    private void pickPostCoverImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Post cover image");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"));
        Stage owner = createPostBox != null && createPostBox.getScene() != null
                ? (Stage) createPostBox.getScene().getWindow() : null;
        File chosen = fc.showOpenDialog(owner);
        if (chosen == null) return;
        try {
            File uploads = new File("uploads");
            if (!uploads.exists()) uploads.mkdirs();
            String orig = chosen.getName().replaceAll("\\s+", "_");
            String fname = System.currentTimeMillis() + "_post_" + orig;
            Files.copy(chosen.toPath(), Paths.get("uploads", fname), StandardCopyOption.REPLACE_EXISTING);
            pendingPostImageFilename = fname;
            refreshPostImageHint();
        } catch (Exception ex) {
            pendingPostImageFilename = null;
            if (postImageHint != null) postImageHint.setText("⚠  " + ex.getMessage());
        }
    }

    private void refreshPostImageHint() {
        if (postImageHint == null) return;
        postImageHint.setText(pendingPostImageFilename == null
                ? "No image — cover will use title gradient only."
                : "Attached: " + pendingPostImageFilename);
    }

    private void expandCreateBox(String title, String body) {
        if (postCollapsedBox != null) { postCollapsedBox.setVisible(false); postCollapsedBox.setManaged(false); }
        if (postExpandedBox  != null) { postExpandedBox.setVisible(true);   postExpandedBox.setManaged(true);  }
        if (postTitleInput != null) {
            if (title != null) postTitleInput.setText(title);
            postTitleInput.requestFocus();
        }
        if (postBodyInput != null && body != null) postBodyInput.setText(body);
        refreshPostImageHint();
    }

    // ── Load data ─────────────────────────────────────────────────────

    private void loadDataAsync() {
        new Thread(() -> {
            try { localPosts = postService.recuperer(); } catch (Exception e) { localPosts = new ArrayList<>(); }

            for (Post p : localPosts) {
                ReactionService.seed("post:" + p.getId(), (long) (Math.random() * 30 + 5),
                        (long) (Math.random() * 15 + 2), (long) (Math.random() * 8),
                        (long) (Math.random() * 12 + 1));
            }

            Platform.runLater(() -> newsStatusLabel.setText("Fetching music news from Dev.to…"));
            newsArticles = MusicNewsService.getLatestNews();

            String[] quote = QuoteService.getRandomQuote();
            MusicTriviaService.TriviaQuestion trivia = MusicTriviaService.getMusicTrivia();

            Platform.runLater(() -> {
                newsStatusLabel.setText(newsArticles.isEmpty()
                        ? "No live articles (offline?)"
                        : newsArticles.size() + " articles loaded");
                quoteText.setText("\"" + quote[0] + "\"");
                quoteAuthor.setText("— " + quote[1]);
                buildNewsSidebar();
                renderTrivia(trivia);
                buildFeed();
            });
        }, "blog-loader").start();
    }

    private void renderTrivia(MusicTriviaService.TriviaQuestion q) {
        if (triviaBox == null || q == null) return;
        triviaBox.setVisible(true); triviaBox.setManaged(true);
        triviaQuestion.setText(q.question());
        triviaAnswers.getChildren().clear();
        triviaResult.setText("Pick an answer:");

        for (String answer : q.shuffledAnswers()) {
            Button b = new Button(answer);
            b.getStyleClass().add("reaction-btn");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setWrapText(true);
            b.setOnAction(ev -> {
                boolean correct = answer.equals(q.correctAnswer());
                triviaResult.setText(correct
                        ? "✅  Correct! Great ear."
                        : "❌  Not quite — answer: " + q.correctAnswer());
                for (javafx.scene.Node n : triviaAnswers.getChildren()) {
                    if (n instanceof Button bb) {
                        bb.setDisable(true);
                        if (q.correctAnswer().equals(bb.getText())) {
                            bb.getStyleClass().add("reaction-btn-active");
                        }
                    }
                }
            });
            triviaAnswers.getChildren().add(b);
        }
    }

    // ── News sidebar ──────────────────────────────────────────────────

    private void buildNewsSidebar() {
        newsSidebar.getChildren().clear();
        if (newsArticles == null || newsArticles.isEmpty()) {
            Label empty = new Label("No headlines right now. Tap EXPLORE to retry in the browser.");
            empty.getStyleClass().add("sidebar-status-label");
            empty.setWrapText(true);
            newsSidebar.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < newsArticles.size(); i++) {
            MusicNewsService.Article a = newsArticles.get(i);
            int idx = i;
            VBox item = new VBox(4);
            item.getStyleClass().add("news-sidebar-item");
            item.setOnMouseClicked(e -> showNewsArticle(a, idx));

            Label title = new Label(a.title().length() > 60 ? a.title().substring(0, 60) + "…" : a.title());
            title.getStyleClass().add("news-sidebar-title");
            title.setWrapText(true);
            Label meta = new Label(a.author() + " · " + a.publishedAt());
            meta.getStyleClass().add("news-sidebar-meta");
            Label reactions = new Label("👍 " + a.reactions() + "  💬 " + a.comments());
            reactions.getStyleClass().add("news-sidebar-stats");
            item.getChildren().addAll(title, meta, reactions);
            newsSidebar.getChildren().add(item);
        }
    }

    /** Wider in-app news reader: list + WebView, dimmed backdrop, click outside to close. */
    @FXML
    void openMusicNewsBrowser() {
        new Thread(() -> {
            List<MusicNewsService.Article> arts = MusicNewsService.getLatestNews(28);
            Platform.runLater(() -> showNewsExplorerModal(arts));
        }, "news-explorer").start();
    }

    private void showNewsExplorerModal(List<MusicNewsService.Article> articles) {
        BorderPane root = new BorderPane();
        Label hint = new Label("Choose a story on the left — browse Dev.to on the right. Click the dark area outside this panel to close.");
        hint.getStyleClass().add("sidebar-status-label");
        hint.setWrapText(true);
        VBox top = new VBox(hint);
        top.setPadding(new Insets(0, 0, 10, 4));

        VBox list = new VBox(10);
        list.setPadding(new Insets(4));
        ScrollPane listScroll = new ScrollPane(list);
        listScroll.setFitToWidth(true);
        listScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");

        WebView web = new WebView();
        web.setPrefSize(480, 380);

        if (articles == null || articles.isEmpty()) {
            list.getChildren().add(new Label("No articles returned. Check your connection."));
        } else {
            for (MusicNewsService.Article a : articles) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(8));
                row.setStyle("-fx-background-color: rgba(128,128,128,0.08);-fx-background-radius:10;-fx-cursor:hand;");

                StackPane thumb = buildNewsVisual(a.title(), a.coverImage(), 76, 76, 14);

                VBox txt = new VBox(4);
                HBox.setHgrow(txt, Priority.ALWAYS);
                Label tl = new Label(a.title().length() > 115 ? a.title().substring(0, 112) + "…" : a.title());
                tl.getStyleClass().add("news-sidebar-title");
                tl.setWrapText(true);
                Label meta = new Label(a.author() + " · " + a.publishedAt());
                meta.getStyleClass().add("news-sidebar-meta");
                String d = a.description() == null ? "" : a.description();
                Label ex = new Label(d.length() > 140 ? d.substring(0, 137) + "…" : d);
                ex.getStyleClass().add("feed-post-content");
                ex.setWrapText(true);
                txt.getChildren().addAll(tl, meta, ex);

                String url = a.url();
                row.setOnMouseClicked(ev -> web.getEngine().load(url));
                row.getChildren().addAll(thumb, txt);
                list.getChildren().add(row);
            }
            web.getEngine().load(articles.get(0).url());
        }

        SplitPane split = new SplitPane(listScroll, web);
        split.setDividerPositions(0.38);
        VBox.setVgrow(split, Priority.ALWAYS);
        root.setTop(top);
        root.setCenter(split);

        OverlayService.showPanel("Music news browser", root, 920, 640);
    }

    // ── Feed (database posts only) ────────────────────────────────────

    @FXML void filterFeed() { buildFeed(); }

    private void buildFeed() {
        feedList.getChildren().clear();
        String q = searchField.getText().toLowerCase().trim();
        String mode = filterCombo.getValue();

        List<Post> posts = localPosts == null ? new ArrayList<>() : localPosts.stream()
                .filter(p -> q.isEmpty()
                        || (p.getTitle() != null && p.getTitle().toLowerCase().contains(q))
                        || (p.getContent() != null && p.getContent().toLowerCase().contains(q)))
                .collect(Collectors.toList());

        if ("Most Reactions".equals(mode)) {
            posts.sort((a, b) -> Long.compare(
                    ReactionService.totalReactions("post:" + b.getId()),
                    ReactionService.totalReactions("post:" + a.getId())));
        } else {
            posts.sort(Comparator.comparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        countLabel.setText(posts.size() + " post" + (posts.size() == 1 ? "" : "s"));

        for (int i = 0; i < posts.size(); i++) {
            VBox card = buildPostCard(posts.get(i));
            feedList.getChildren().add(card);
            card.setOpacity(0);
            int delay = i * 40;
            PauseTransition p = new PauseTransition(Duration.millis(delay));
            p.setOnFinished(ev -> {
                FadeTransition ft = new FadeTransition(Duration.millis(250), card);
                ft.setToValue(1);
                ft.play();
            });
            p.play();
        }

        if (posts.isEmpty()) {
            Label empty = new Label("📭  No posts from the database yet. Write the first one above.");
            empty.getStyleClass().add("front-empty-label");
            empty.setStyle("-fx-padding:40 0 0 32;");
            feedList.getChildren().add(empty);
        }
    }

    private static final String[] GRAD_COLORS = {
            "#6366F1", "#E8320A", "#10B981", "#F59E0B", "#8B5CF6", "#0EA5E9", "#F43F5E", "#14B8A6"
    };

    private User userFor(int userId) {
        try {
            return userService.recupererParId(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeName(User u) {
        if (u == null) return "";
        String p = u.getPrenom() != null ? u.getPrenom().trim() : "";
        String n = u.getNom() != null ? u.getNom().trim() : "";
        return (p + " " + n).trim();
    }

    private VBox buildPostCard(Post post) {
        VBox card = new VBox(0);
        card.getStyleClass().add("feed-card");


        User author = userFor(post.getAuthorId());
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("feed-card-header");

        StackPane avWrap = new StackPane();
        avWrap.setMinSize(40, 40);
        avWrap.setPrefSize(40, 40);
        ImageView avImg = new ImageView();
        avImg.setFitWidth(40);
        avImg.setFitHeight(40);
        Circle avClip = new Circle(20, 20, 20);
        avImg.setClip(avClip);
        String initial = "?";
        if (author != null) {
            if (author.getPrenom() != null && !author.getPrenom().isBlank()) {
                initial = author.getPrenom().substring(0, 1).toUpperCase();
            } else if (author.getNom() != null && !author.getNom().isBlank()) {
                initial = author.getNom().substring(0, 1).toUpperCase();
            }
        }
        Label avFallback = new Label(initial);
        avFallback.setStyle("-fx-font-size:14px;-fx-font-weight:700;-fx-text-fill:#C8CDD8;");
        if (author != null) {
            String avUrl = GravatarService.resolveAvatarUrl(author, 96);
            PlaceholderImageService.loadInto(avImg, avUrl, null);
            avImg.imageProperty().addListener((o, ov, nv) -> {
                if (nv != null && !nv.isError()) avFallback.setVisible(false);
            });
        }
        avWrap.getChildren().addAll(avImg, avFallback);

        VBox authorInfo = new VBox(1);
        String displayName = safeName(author);
        if (displayName.isBlank()) displayName = "User #" + post.getAuthorId();
        Label authorName = new Label(displayName);
        authorName.getStyleClass().add("feed-author");
        String dateStr = post.getCreatedAt() != null ? post.getCreatedAt().format(DF) : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("feed-date");
        authorInfo.getChildren().addAll(authorName, dateLabel);

        int words = wordCount(post.getContent());
        int mins = Math.max(1, words / 200);
        long reactions = ReactionService.totalReactions("post:" + post.getId());
        int commentCnt = 0;
        try {
            commentCnt = (int) commentService.recuperer().stream().filter(c -> c.getPostId() == post.getId()).count();
        } catch (Exception ignored) { }
        long engScore = reactions * 2L + commentCnt * 3L;
        String engLabel = engScore >= 30 ? "🔥 Hot" : engScore >= 10 ? "⭐ Trending" : "💬 New";
        Label rt = new Label("⏱ " + mins + " min  ·  " + engLabel);
        rt.getStyleClass().add("feed-read-time");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        header.getChildren().addAll(avWrap, authorInfo, sp, rt);

        VBox body = new VBox(8);
        body.getStyleClass().add("feed-card-body");
        Label title = new Label(post.getTitle() != null ? post.getTitle() : "Untitled");
        title.getStyleClass().add("feed-post-title");
        title.setWrapText(true);
        String preview = post.getContent() != null
                ? (post.getContent().length() > 220 ? post.getContent().substring(0, 220) + "…" : post.getContent())
                : "";
        Label contentLbl = new Label(preview);
        contentLbl.getStyleClass().add("feed-post-content");
        contentLbl.setWrapText(true);
        body.getChildren().addAll(title, contentLbl);

        VBox footer = buildFeedFooter("post:" + post.getId(), post.getId(), true);
        card.getChildren().addAll(header, body, footer);
        return card;
    }

    private VBox buildFeedFooter(String key, int id, boolean isPost) {
        VBox footer = new VBox(0);
        footer.getStyleClass().add("feed-footer");

        HBox reactionBar = new HBox(4);
        reactionBar.getStyleClass().add("feed-reaction-bar");
        reactionBar.setAlignment(Pos.CENTER_LEFT);

        for (ReactionService.Type type : ReactionService.Type.values()) {
            VBox btn = buildReactionBtn(key, type);
            reactionBar.getChildren().add(btn);
        }

        Region rsp = new Region();
        HBox.setHgrow(rsp, Priority.ALWAYS);

        Button commentBtn = new Button("💬  Comments");
        commentBtn.getStyleClass().add("feed-comment-toggle");

        VBox commentSection = new VBox(8);
        commentSection.getStyleClass().add("feed-comment-section");
        commentSection.setVisible(false);
        commentSection.setManaged(false);
        if (isPost) buildCommentSection(commentSection, id);

        commentBtn.setOnAction(e -> {
            boolean open = !commentSection.isVisible();
            commentSection.setVisible(open);
            commentSection.setManaged(open);
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

        int userId = currentUser != null ? currentUser.getId() : Integer.MIN_VALUE;
        if (userId != Integer.MIN_VALUE && type.equals(ReactionService.getUserReaction(key, userId)))
            lbl.getStyleClass().add("reaction-btn-active");

        VBox box = new VBox(lbl);
        box.setAlignment(Pos.CENTER);
        box.setOnMouseClicked(e -> {
            if (userId == Integer.MIN_VALUE) return;
            ReactionService.react(key, userId, type);
            long newCount = ReactionService.getCounts(key).getOrDefault(type, 0L);
            lbl.setText(type.emoji() + "  " + newCount);
            boolean active = type.equals(ReactionService.getUserReaction(key, userId));
            if (active) {
                if (!lbl.getStyleClass().contains("reaction-btn-active")) lbl.getStyleClass().add("reaction-btn-active");
            } else lbl.getStyleClass().remove("reaction-btn-active");

            ScaleTransition bounce = new ScaleTransition(Duration.millis(120), lbl);
            bounce.setToX(1.3);
            bounce.setToY(1.3);
            bounce.setCycleCount(2);
            bounce.setAutoReverse(true);
            bounce.play();
        });
        return box;
    }

    private void buildCommentSection(VBox section, int postId) {
        try {
            List<Comment> comments = commentService.recuperer().stream()
                    .filter(c -> c.getPostId() == postId)
                    .sorted(Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(12)
                    .collect(Collectors.toList());

            if (!comments.isEmpty()) {
                VBox list = new VBox(6);
                for (Comment c : comments) {
                    HBox row = new HBox(8);
                    row.setAlignment(Pos.TOP_LEFT);

                    User cu = userFor(c.getAuthorId());
                    StackPane avWrap = new StackPane();
                    avWrap.setMinSize(32, 32);
                    avWrap.setPrefSize(32, 32);
                    ImageView cAv = new ImageView();
                    cAv.setFitWidth(32);
                    cAv.setFitHeight(32);
                    Circle cClip = new Circle(16, 16, 16);
                    cAv.setClip(cClip);
                    String ini = "?";
                    if (cu != null) {
                        if (cu.getPrenom() != null && !cu.getPrenom().isBlank()) {
                            ini = cu.getPrenom().substring(0, 1).toUpperCase();
                        } else if (cu.getNom() != null && !cu.getNom().isBlank()) {
                            ini = cu.getNom().substring(0, 1).toUpperCase();
                        }
                    }
                    Label cFb = new Label(ini);
                    cFb.setStyle("-fx-font-size:11px;-fx-font-weight:700;-fx-text-fill:#C8CDD8;");
                    if (cu != null) {
                        String avUrl = GravatarService.resolveLocalUploadAvatarUrl(cu);
                        if (avUrl != null) {
                            PlaceholderImageService.loadInto(cAv, avUrl, null);
                            cAv.imageProperty().addListener((o, ov, nv) -> {
                                if (nv != null && !nv.isError()) cFb.setVisible(false);
                            });
                        }
                    }
                    avWrap.getChildren().addAll(cAv, cFb);

                    VBox cb = new VBox(2);
                    Label who = new Label(safeName(cu).isBlank() ? ("User #" + c.getAuthorId()) : safeName(cu));
                    who.getStyleClass().add("feed-date");
                    who.setStyle(who.getStyle() + ";-fx-font-weight:600;");
                    Label ct = new Label(c.getContent());
                    ct.getStyleClass().add("feed-comment-text");
                    ct.setWrapText(true);
                    String cd = c.getCreatedAt() != null ? c.getCreatedAt().format(DF) : "";
                    Label dt = new Label(cd);
                    dt.getStyleClass().add("feed-comment-date");
                    cb.getChildren().addAll(who, ct, dt);
                    HBox.setHgrow(cb, Priority.ALWAYS);
                    row.getChildren().addAll(avWrap, cb);
                    list.getChildren().add(row);
                }
                section.getChildren().add(list);
            }
        } catch (Exception ignored) { }

        if (currentUser != null) {
            HBox inputRow = new HBox(8);
            inputRow.setAlignment(Pos.CENTER_LEFT);
            inputRow.getStyleClass().add("feed-comment-input-row");
            TextField input = new TextField();
            input.setPromptText("Write a comment…");
            input.getStyleClass().add("feed-comment-input");
            HBox.setHgrow(input, Priority.ALWAYS);
            Button post = new Button("POST");
            post.getStyleClass().add("feed-comment-post-btn");
            post.setOnAction(e -> {
                String text = input.getText().trim();
                if (text.isEmpty()) return;
                try {
                    Comment cm = new Comment(postId, currentUser.getId(), text);
                    commentService.ajouter(cm);
                    input.clear();
                    section.getChildren().clear();
                    buildCommentSection(section, postId);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
            inputRow.getChildren().addAll(input, post);
            section.getChildren().add(inputRow);
        }
    }

    /** News thumbnail / hero: Dev.to cover when available, otherwise gradient + initial (no LoremFlickr). */
    private StackPane buildNewsVisual(String title, String coverUrl, double w, double h, double arc) {
        String col = GRAD_COLORS[Math.abs((title != null ? title : "").hashCode()) % GRAD_COLORS.length];
        Region bg = new Region();
        bg.setMinSize(w, h);
        bg.setPrefSize(w, h);
        bg.setMaxSize(w, h);
        bg.setStyle("-fx-background-color:linear-gradient(to bottom right," + col + "AA," + col + "33);"
                + "-fx-background-radius:" + arc + ";");
        String letter = (title != null && !title.isBlank()) ? title.substring(0, 1).toUpperCase() : "N";
        double fontPx = h <= 80 ? 22 : 48;
        Label init = new Label(letter);
        init.setStyle("-fx-font-size:" + fontPx + "px;-fx-font-weight:700;-fx-text-fill:#FFFFFF;-fx-opacity:0.55;");
        StackPane pane = new StackPane();
        pane.getChildren().add(bg);
        if (coverUrl != null && !coverUrl.isBlank()) {
            ImageView iv = new ImageView();
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            try {
                Image img = new Image(coverUrl, (int) w, (int) h, false, true, true);
                iv.setImage(img);
                pane.getChildren().add(iv);
                iv.imageProperty().addListener((o, ov, nv) -> {
                    if (nv != null && !nv.isError()) init.setVisible(false);
                });
            } catch (Exception ignored) { }
        }
        pane.getChildren().add(init);
        Rectangle clip = new Rectangle(w, h);
        clip.setArcWidth(arc * 2);
        clip.setArcHeight(arc * 2);
        pane.setClip(clip);
        return pane;
    }

    private void showNewsArticle(MusicNewsService.Article a, int idx) {
        VBox content = new VBox(14);
        content.getStyleClass().add("overlay-body");
        StackPane hero = buildNewsVisual(a.title(), a.coverImage(), 480, 160, 20);

        Label src = new Label("📡 " + a.author() + " · " + a.publishedAt());
        src.getStyleClass().add("overlay-post-meta");
        Label title = new Label(a.title());
        title.getStyleClass().add("overlay-post-title");
        title.setWrapText(true);
        Label desc = new Label(a.description());
        desc.getStyleClass().add("overlay-post-body");
        desc.setWrapText(true);
        HBox stats = new HBox(16);
        Label r = new Label("👍 " + a.reactions() + " reactions");
        r.getStyleClass().add("feed-read-time");
        Label cm = new Label("💬 " + a.comments() + " comments");
        cm.getStyleClass().add("feed-read-time");
        stats.getChildren().addAll(r, cm);
        Label link = new Label("🔗 " + a.url());
        link.getStyleClass().add("comment-date");
        link.setWrapText(true);

        HBox actions = new HBox(10);
        Button openInApp = new Button("🌐  Read inside app");
        openInApp.getStyleClass().add("card-action-btn");
        openInApp.setOnAction(e -> {
            OverlayService.hide();
            showNewsExplorerModal(List.of(a));
        });
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
            String preTitle = a.title();
            String preBody = "📰 " + a.description() + "\n\nSource: " + a.url();
            expandCreateBox(preTitle, preBody);
        });
        actions.getChildren().addAll(openInApp, copyUrl, draftPost);

        content.getChildren().addAll(hero, src, title, desc, stats, link, actions);
        OverlayService.show("Music News", content);
    }

    private int wordCount(String t) {
        return (t == null || t.isBlank()) ? 0 : t.trim().split("\\s+").length;
    }
}
