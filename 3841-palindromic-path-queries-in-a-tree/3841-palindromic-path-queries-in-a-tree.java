import java.util.*;

/*
 * LeetCode 3841 - Palindromic Path Queries in a Tree
 *
 * A path can be rearranged into a palindrome  <=>  at most one letter has an odd count
 *                                             <=>  XOR of the 26-bit letter masks on the path has <= 1 set bit.
 *
 *   pathMask(u, v) = P[u] ^ P[v] ^ mask[lca(u, v)],   P[x] = XOR of masks on root..x
 *
 *  - Nodes are relabelled in preorder, so every subtree is one contiguous block [i, end[i]].
 *  - Changing the letter of node i XORs a delta onto P[] of that whole block, so P[] lives in a
 *    Fenwick tree (XOR) with range-update / point-query: O(log n).
 *  - LCA by heavy-path jumping (tree is static): O(log n), tiny constants, O(n) memory.
 *  - The tree is built by leaf peeling (degree + XOR of neighbours): no adjacency lists, no recursion.
 *
 * Time: O((n + q) log n)    Space: O(n)
 */
class Solution {
    public List<Boolean> palindromePath(int n, int[][] edges, String s, String[] queries) {
        // ---------- 1. degree + XOR of neighbours (no adjacency lists at all) ----------
        final int[] deg = new int[n];
        final int[] xr = new int[n];
        for (int[] e : edges) {
            final int a = e[0], b = e[1];
            deg[a]++;
            deg[b]++;
            xr[a] ^= b;
            xr[b] ^= a;
        }

        // ---------- 2. peel leaves: parent, subtree size, heavy child ----------
        final int[] parent = new int[n];
        final int[] sub = new int[n];    // number of proper descendants
        final int[] order = new int[n];  // removal order (children always before parents)
        final int[] heavy = new int[n];  // heavy child label + 1 (0 = none yet)
        int k = 0;
        for (int i = 0; i < n; i++) {
            int v = i;
            while (deg[v] == 1) {
                final int p = xr[v];     // the only remaining neighbour = parent
                deg[v] = 0;
                parent[v] = p;
                order[k++] = v;
                xr[p] ^= v;
                deg[p]--;
                sub[p] += sub[v] + 1;
                final int h = heavy[p];
                if (h == 0 || sub[v] > sub[h - 1]) heavy[p] = v + 1;
                if (p > i) break;        // outer loop will reach p later
                v = p;                   // p is behind the outer loop: continue peeling here
            }
        }
        final int root = n == 1 ? 0 : parent[order[n - 2]];

        // ---------- 3. top-down: preorder index (subtree = contiguous block), chains, prefix masks ----------
        final int[] tin = new int[n];    // label -> preorder index
        final int[] nxt = new int[n];    // label -> next free index for its children's blocks
        final int[] bit = new int[n];    // preorder idx -> 1 << letter
        final int[] end = new int[n];    // preorder idx -> last preorder idx in its subtree
        final int[] head = new int[n];   // preorder idx -> preorder idx of its heavy-chain head
        final int[] up = new int[n];     // preorder idx -> preorder idx of the parent of that head
        final int[] tree = new int[n + 1];

        final int m0 = 1 << (s.charAt(root) - 'a');
        nxt[root] = 1;
        bit[0] = m0;
        tree[1] = m0;                    // temporarily: XOR of masks root..node (1-indexed)
        end[0] = n - 1;
        for (int t = n - 2; t >= 0; t--) {
            final int v = order[t];
            final int p = parent[v];
            final int pid = tin[p];
            final int sz = sub[v];
            final int id = nxt[p];
            nxt[p] = id + sz + 1;
            tin[v] = id;
            nxt[v] = id + 1;
            final int m = 1 << (s.charAt(v) - 'a');
            bit[id] = m;
            tree[id + 1] = tree[pid + 1] ^ m;
            end[id] = id + sz;
            if (heavy[p] == v + 1) {
                head[id] = head[pid];
                up[id] = up[pid];
            } else {
                head[id] = id;
                up[id] = pid;
            }
        }

        // ---------- 4. turn `tree` into a Fenwick tree (XOR) of the difference array, O(n) ----------
        // afterwards prefixXor(i + 1) == XOR of masks on the root..node(i) path, kept current by updates
        for (int i = n; i >= 2; i--) tree[i] ^= tree[i - 1];
        for (int i = 1; i <= n; i++) {
            final int j = i + (i & -i);
            if (j <= n) tree[j] ^= tree[i];
        }

        // ---------- 5. answer queries ----------
        final List<Boolean> res = new ArrayList<>(queries.length);
        for (String q : queries) {
            if (q.charAt(0) == 'u') {
                // "update u c"
                int p = 7, u = 0, c;
                while ((c = q.charAt(p)) != ' ') {
                    u = u * 10 + (c - '0');
                    p++;
                }
                final int nb = 1 << (q.charAt(p + 1) - 'a');
                final int id = tin[u];
                final int d = bit[id] ^ nb;
                if (d != 0) {
                    bit[id] = nb;
                    // XOR d onto [id, end[id]]: touch positions id+1 and end[id]+2, stop where the chains merge
                    int i = id + 1, j = end[id] + 2;
                    while (i != j) {
                        if (i < j) {
                            if (i > n) break;
                            tree[i] ^= d;
                            i += i & -i;
                        } else {
                            if (j > n) break;
                            tree[j] ^= d;
                            j += j & -j;
                        }
                    }
                }
            } else {
                // "query u v"
                int p = 6, u = 0, v = 0, c;
                final int len = q.length();
                while ((c = q.charAt(p)) != ' ') {
                    u = u * 10 + (c - '0');
                    p++;
                }
                for (p++; p < len; p++) v = v * 10 + (q.charAt(p) - '0');

                final int a = tin[u], b = tin[v];

                // LCA by heavy-path jumping (compare chain heads by preorder index)
                int x = a, y = b;
                int hx = head[x], hy = head[y];
                while (hx != hy) {
                    if (hx > hy) {
                        x = up[x];
                        hx = head[x];
                    } else {
                        y = up[y];
                        hy = head[y];
                    }
                }
                final int w = x < y ? x : y;

                // prefix(a) ^ prefix(b) ^ mask[lca]; the shared Fenwick suffix cancels, so stop where chains meet
                int acc = bit[w];
                int i = a + 1, j = b + 1;
                while (i != j) {
                    if (i > j) {
                        acc ^= tree[i];
                        i &= i - 1;
                    } else {
                        acc ^= tree[j];
                        j &= j - 1;
                    }
                }
                res.add((acc & (acc - 1)) == 0);
            }
        }
        return res;
    }
}