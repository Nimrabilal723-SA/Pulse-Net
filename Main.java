import java.util.*;


// User: stores a single user's data (node in the Graph)
class User implements Comparable<User> {
    String userId;
    String username;
    String displayName;
    int followerCount;
    ArrayList<Post> posts;

    User(String userId, String username, String displayName) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.followerCount = 0;
        this.posts = new ArrayList<>();
    }

    void addPost(Post post) { posts.add(post); }
    void incrementFollowers() { followerCount++; }
    void decrementFollowers() { if (followerCount > 0) followerCount--; }

    @Override
    public int compareTo(User other) {
        return Integer.compare(this.followerCount, other.followerCount);
    }

    @Override
    public String toString() {
        return "[" + username + " | followers: " + followerCount + "]";
    }
}


// Post: stores a single post's data (content, hashtags, engagement score)
class Post implements Comparable<Post> {
    String postId;
    String authorId;
    String content;
    ArrayList<String> hashtags;
    int engagementScore;
    long timestamp;

    Post(String postId, String authorId, String content) {
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.hashtags = new ArrayList<>();
        this.engagementScore = 0;
        this.timestamp = System.currentTimeMillis();
        extractHashtags(content);
    }

    void extractHashtags(String content) {
        for (String word : content.split("\\s+")) {
            if (word.startsWith("#") && word.length() > 1) {
                hashtags.add(word.toLowerCase().substring(1));
            }
        }
    }

    void incrementEngagement() { engagementScore++; }

    @Override
    public int compareTo(Post other) {
        return Integer.compare(other.engagementScore, this.engagementScore);
    }

    @Override
    public String toString() {
        return "Post[" + postId + "] by " + authorId
                + " | score:" + engagementScore
                + " | \"" + content.substring(0, Math.min(45, content.length())) + "\"";
    }
}


// SocialGraph: users = nodes, follow relationships = edges; includes BFS, DFS, and HashSet-based duplicate prevention
class SocialGraph {
    HashMap<String, HashSet<String>> adjacencyList;
    HashMap<String, HashSet<String>> reverseList;

    SocialGraph() {
        adjacencyList = new HashMap<>();
        reverseList   = new HashMap<>();
    }

    void addUser(String userId) {
        adjacencyList.putIfAbsent(userId, new HashSet<>());
        reverseList.putIfAbsent(userId, new HashSet<>());
    }

    // HashSet blocks duplicate follows
    boolean follow(String followerId, String followingId) {
        if (!adjacencyList.containsKey(followerId) || !adjacencyList.containsKey(followingId)) return false;
        boolean added = adjacencyList.get(followerId).add(followingId);
        if (added) {
            reverseList.get(followingId).add(followerId);
            System.out.println("  [Graph] " + followerId + " -> " + followingId + " (follow kiya)");
        } else {
            System.out.println("  [HashSet] BLOCKED! Pehle se follow kar rakha hai.");
        }
        return added;
    }

    void unfollow(String followerId, String followingId) {
        adjacencyList.get(followerId).remove(followingId);
        reverseList.get(followingId).remove(followerId);
        System.out.println("  [Graph] " + followerId + " ne unfollow kiya " + followingId);
    }

    // BFS: post spread level by level
    Map<Integer, List<String>> bfsPostSpread(String startUserId) {
        System.out.println("\n  [BFS] Post spread simulation — " + startUserId + " se shuru:");
        Map<Integer, List<String>> levelMap = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startUserId);
        visited.add(startUserId);
        int level = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<String> currentLevel = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                currentLevel.add(current);
                for (String follower : reverseList.getOrDefault(current, new HashSet<>())) {
                    if (!visited.contains(follower)) {
                        visited.add(follower);
                        queue.add(follower);
                    }
                }
            }
            levelMap.put(level++, currentLevel);
        }
        for (Map.Entry<Integer, List<String>> e : levelMap.entrySet())
            System.out.println("    Hop " + e.getKey() + ": " + e.getValue());
        return levelMap;
    }

    // DFS: full influence circle
    Set<String> dfsInfluenceCircle(String userId) {
        System.out.println("\n  [DFS] Influence circle of: " + userId);
        Set<String> visited = new LinkedHashSet<>();
        dfsHelper(userId, visited);
        visited.remove(userId);
        System.out.println("  [DFS] Result: " + visited);
        return visited;
    }

    private void dfsHelper(String userId, Set<String> visited) {
        visited.add(userId);
        for (String next : adjacencyList.getOrDefault(userId, new HashSet<>())) {
            if (!visited.contains(next)) dfsHelper(next, visited);
        }
    }

    Set<String> getFollowers(String userId) { return reverseList.getOrDefault(userId, new HashSet<>()); }
}


// Trie: powers hashtag and username autocomplete search
class Trie {
    static class TrieNode {
        HashMap<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
        String fullWord = null;
    }

    TrieNode root;
    String type;

    Trie(String type) {
        this.root = new TrieNode();
        this.type = type;
    }

    void insert(String word) {
        word = word.toLowerCase();
        TrieNode cur = root;
        for (char ch : word.toCharArray()) {
            cur.children.putIfAbsent(ch, new TrieNode());
            cur = cur.children.get(ch);
        }
        cur.isEnd = true;
        cur.fullWord = word;
    }

    List<String> autocomplete(String prefix) {
        prefix = prefix.toLowerCase();
        List<String> results = new ArrayList<>();
        TrieNode cur = root;
        for (char ch : prefix.toCharArray()) {
            if (!cur.children.containsKey(ch)) {
                System.out.println("  [Trie] '" + prefix + "' — koi result nahi mila");
                return results;
            }
            cur = cur.children.get(ch);
        }
        collect(cur, results);
        System.out.println("  [Trie] Autocomplete '" + prefix + "' (" + type + "): " + results);
        return results;
    }

    private void collect(TrieNode node, List<String> results) {
        if (node.isEnd) results.add(node.fullWord);
        for (TrieNode child : node.children.values()) collect(child, results);
    }
}


// UserBST: stores users sorted by follower count using a Binary Search Tree
class UserBST {
    static class BSTNode {
        User user;
        BSTNode left, right;
        BSTNode(User u) { user = u; }
    }

    BSTNode root;

    void insert(User user) { root = insertHelper(root, user); }

    private BSTNode insertHelper(BSTNode node, User user) {
        if (node == null) return new BSTNode(user);
        if (user.followerCount < node.user.followerCount)
            node.left = insertHelper(node.left, user);
        else
            node.right = insertHelper(node.right, user);
        return node;
    }

    void printSorted() {
        List<User> result = new ArrayList<>();
        inOrder(root, result);
        System.out.println("  [BST] Users sorted by followers (highest first):");
        for (int i = result.size() - 1; i >= 0; i--)
            System.out.println("    #" + (result.size() - i) + " " + result.get(i));
    }

    private void inOrder(BSTNode node, List<User> result) {
        if (node == null) return;
        inOrder(node.left, result);
        result.add(node.user);
        inOrder(node.right, result);
    }
}


// MergeSort: sorts hashtags by frequency to generate the Top 10 trending report
class MergeSort {
    static class HashtagEntry {
        String hashtag;
        int count;
        HashtagEntry(String h, int c) { hashtag = h; count = c; }
        public String toString() { return "#" + hashtag + "(" + count + " uses)"; }
    }

    static List<HashtagEntry> sortDesc(List<HashtagEntry> list) {
        if (list.size() <= 1) return list;
        int mid = list.size() / 2;
        List<HashtagEntry> left  = sortDesc(new ArrayList<>(list.subList(0, mid)));
        List<HashtagEntry> right = sortDesc(new ArrayList<>(list.subList(mid, list.size())));
        return merge(left, right);
    }

    private static List<HashtagEntry> merge(List<HashtagEntry> l, List<HashtagEntry> r) {
        List<HashtagEntry> res = new ArrayList<>();
        int i = 0, j = 0;
        while (i < l.size() && j < r.size())
            res.add(l.get(i).count >= r.get(j).count ? l.get(i++) : r.get(j++));
        while (i < l.size()) res.add(l.get(i++));
        while (j < r.size()) res.add(r.get(j++));
        return res;
    }
}


// UserService: handles user registration, follow/unfollow, rankings, and activity history (HashMap, Stack, BST, Trie, ArrayList)
class UserService {
    HashMap<String, User> userMap;
    Stack<String> activityStack;
    UserBST userBST;
    Trie usernameTrie;
    ArrayList<User> allUsers;
    SocialGraph graph;

    UserService(SocialGraph graph) {
        this.graph = graph;
        userMap       = new HashMap<>();
        activityStack = new Stack<>();
        userBST       = new UserBST();
        usernameTrie  = new Trie("username");
        allUsers      = new ArrayList<>();
    }

    User registerUser(String userId, String username, String displayName) {
        if (userMap.containsKey(userId)) return userMap.get(userId);
        User user = new User(userId, username, displayName);
        userMap.put(userId, user);
        allUsers.add(user);
        graph.addUser(userId);
        usernameTrie.insert(username);
        activityStack.push("REGISTER:" + username);
        System.out.println("  [HashMap] New user: " + user);
        return user;
    }

    void follow(String followerId, String followingId) {
        User follower  = userMap.get(followerId);
        User following = userMap.get(followingId);
        if (follower == null || following == null) return;
        boolean success = graph.follow(followerId, followingId);
        if (success) {
            following.incrementFollowers();
            userBST.insert(following);
            activityStack.push("FOLLOW:" + followerId + "->" + followingId);
        }
    }

    void unfollow(String followerId, String followingId) {
        User following = userMap.get(followingId);
        if (following != null) {
            graph.unfollow(followerId, followingId);
            following.decrementFollowers();
            activityStack.push("UNFOLLOW:" + followerId + "->" + followingId);
        }
    }

    void undoLastActivity() {
        if (activityStack.isEmpty()) { System.out.println("  [Stack] Kuch nahi undo karna."); return; }
        String last = activityStack.pop();
        System.out.println("  [Stack] UNDO: " + last);
    }

    void printActivityHistory() {
        System.out.println("  [Stack] Activity history (recent first):");
        Stack<String> copy = (Stack<String>) activityStack.clone();
        int i = 1;
        while (!copy.isEmpty()) System.out.println("    " + i++ + ". " + copy.pop());
    }

    List<String> searchUsername(String prefix) { return usernameTrie.autocomplete(prefix); }
    void showUserRankings() { userBST.printSorted(); }
    User getUser(String userId) { return userMap.get(userId); }
}


// PostService: handles post creation, engagement, news feed, and trending posts (PriorityQueue, LinkedList, Trie, MergeSort)
class PostService {
    LinkedList<Post> newsFeed;
    PriorityQueue<Post> trendingQueue;
    Trie hashtagTrie;
    HashMap<String, Integer> hashtagFreq;
    SocialGraph graph;
    int postCounter = 1;

    PostService(SocialGraph graph) {
        this.graph    = graph;
        newsFeed      = new LinkedList<>();
        trendingQueue = new PriorityQueue<>();
        hashtagTrie   = new Trie("hashtag");
        hashtagFreq   = new HashMap<>();
    }

    Post createPost(String authorId, String content) {
        String postId = "P" + String.format("%03d", postCounter++);
        Post post = new Post(postId, authorId, content);
        newsFeed.addFirst(post);
        trendingQueue.offer(post);
        for (String tag : post.hashtags) {
            hashtagTrie.insert(tag);
            hashtagFreq.merge(tag, 1, Integer::sum);
        }
        System.out.println("  [LinkedList] Post added: " + post);
        return post;
    }

    void engagePost(Post post) {
        trendingQueue.remove(post);
        post.incrementEngagement();
        trendingQueue.offer(post);
        System.out.println("  [PriorityQueue] Engagement updated: " + post);
    }

    void printNewsFeed(int limit) {
        System.out.println("  [LinkedList] News Feed (newest first):");
        int count = 0;
        for (Post p : newsFeed) {
            if (count++ >= limit) break;
            System.out.println("    " + p);
        }
    }

    void printTrendingPosts(int n) {
        System.out.println("  [PriorityQueue] Top " + n + " Trending Posts:");
        PriorityQueue<Post> copy = new PriorityQueue<>(trendingQueue);
        for (int i = 1; !copy.isEmpty() && i <= n; i++)
            System.out.println("    #" + i + " " + copy.poll());
    }

    void printTop10Hashtags() {
        List<MergeSort.HashtagEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : hashtagFreq.entrySet())
            entries.add(new MergeSort.HashtagEntry(e.getKey(), e.getValue()));
        List<MergeSort.HashtagEntry> sorted = MergeSort.sortDesc(entries);
        System.out.println("  [MergeSort] Top 10 Trending Hashtags:");
        for (int i = 0; i < Math.min(10, sorted.size()); i++)
            System.out.println("    #" + (i+1) + " " + sorted.get(i));
    }

    List<String> searchHashtag(String prefix) { return hashtagTrie.autocomplete(prefix); }
    void simulatePostSpread(String authorId) { graph.bfsPostSpread(authorId); }
    void traceInfluenceCircle(String userId) { graph.dfsInfluenceCircle(userId); }
}


// PulseNet: main class — interactive menu-driven program covering all 12 data structures
public class Main {

    static Scanner sc = new Scanner(System.in);
    static SocialGraph graph = new SocialGraph();
    static UserService users = new UserService(graph);
    static PostService posts = new PostService(graph);

    public static void main(String[] args) {

        sep("Welcome to PulseNet");
        System.out.println("  Real-Time Social Network Trend & Influence Tracker");

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("  Enter your choice: ");

            switch (choice) {
                case 1  -> registerUserMenu();
                case 2  -> followUserMenu();
                case 3  -> unfollowUserMenu();
                case 4  -> createPostMenu();
                case 5  -> engagePostMenu();
                case 6  -> posts.printNewsFeed(10);
                case 7  -> { int n = readInt("  Top how many trending posts? "); posts.printTrendingPosts(n); }
                case 8  -> { String id = readLine("  Enter author User ID (e.g. U001): "); posts.simulatePostSpread(id); }
                case 9  -> { String id = readLine("  Enter User ID (e.g. U001): "); posts.traceInfluenceCircle(id); }
                case 10 -> { String pre = readLine("  Enter hashtag prefix: "); posts.searchHashtag(pre); }
                case 11 -> { String pre = readLine("  Enter username prefix: "); users.searchUsername(pre); }
                case 12 -> users.showUserRankings();
                case 13 -> users.printActivityHistory();
                case 14 -> users.undoLastActivity();
                case 15 -> posts.printTop10Hashtags();
                case 0  -> { running = false; System.out.println("\n  Exiting PulseNet. Thank you!"); }
                default -> System.out.println("  Invalid choice, please try again.");
            }
        }

        sc.close();
    }

    static void printMenu() {
        sep("PulseNet Menu");
        System.out.println("   1.  Register a new user            [HashMap + ArrayList + Trie]");
        System.out.println("   2.  Follow a user                  [Graph + HashSet]");
        System.out.println("   3.  Unfollow a user                [Graph]");
        System.out.println("   4.  Create a post                  [LinkedList + PriorityQueue + Trie + HashMap]");
        System.out.println("   5.  Engage with a post (like)      [PriorityQueue]");
        System.out.println("   6.  View news feed                 [LinkedList]");
        System.out.println("   7.  View top trending posts        [PriorityQueue]");
        System.out.println("   8.  Simulate post spread           [BFS]");
        System.out.println("   9.  View user's influence circle   [DFS]");
        System.out.println("  10.  Search hashtags (autocomplete) [Trie]");
        System.out.println("  11.  Search usernames (autocomplete)[Trie]");
        System.out.println("  12.  View user rankings by followers[BST]");
        System.out.println("  13.  View activity history          [Stack]");
        System.out.println("  14.  Undo last activity              [Stack]");
        System.out.println("  15.  View top 10 trending hashtags  [MergeSort]");
        System.out.println("   0.  Exit");
    }

    // ---- Menu actions ----

    static void registerUserMenu() {
        sep("Register New User");
        String id   = readLine("  Enter User ID (e.g. U007): ");
        String uname = readLine("  Enter username: ");
        String dname = readLine("  Enter display name: ");
        users.registerUser(id, uname, dname);
    }

    static void followUserMenu() {
        sep("Follow a User");
        String from = readLine("  Enter your User ID: ");
        String to   = readLine("  Enter User ID to follow: ");
        users.follow(from, to);
    }

    static void unfollowUserMenu() {
        sep("Unfollow a User");
        String from = readLine("  Enter your User ID: ");
        String to   = readLine("  Enter User ID to unfollow: ");
        users.unfollow(from, to);
    }

    static void createPostMenu() {
        sep("Create a Post");
        String id      = readLine("  Enter your User ID: ");
        String content = readLine("  Enter post content (use #hashtags): ");
        posts.createPost(id, content);
    }

    static void engagePostMenu() {
        sep("Engage With a Post");
        String id = readLine("  Enter Post ID (e.g. P001): ");
        for (Post p : posts.newsFeed) {
            if (p.postId.equalsIgnoreCase(id)) {
                posts.engagePost(p);
                return;
            }
        }
        System.out.println("  Post not found.");
    }

    // ---- Input helpers ----

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static void sep(String title) {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("  " + title);
        System.out.println("=".repeat(55));
    }
}
