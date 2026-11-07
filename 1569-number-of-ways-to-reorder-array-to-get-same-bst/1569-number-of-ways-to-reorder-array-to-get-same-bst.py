class Solution:
    def numOfWays(self, nums):
        MOD = 10**9 + 7
        n = len(nums)

        # factorials and inverse factorials
        fact = [1] * (n + 1)
        for i in range(1, n + 1):
            fact[i] = fact[i - 1] * i % MOD
        inv = [1] * (n + 1)
        inv[n] = pow(fact[n], MOD - 2, MOD)
        for i in range(n, 0, -1):
            inv[i - 1] = inv[i] * i % MOD

        # pos[v] = insertion index of value v (v = 1..n)
        pos = [0] * (n + 1)
        for i, v in enumerate(nums):
            pos[v] = i

        # Cartesian tree over values 1..n, min-heap on pos (earlier = ancestor)
        left = [0] * (n + 1)
        right = [0] * (n + 1)
        stack = []
        for v in range(1, n + 1):
            last = 0
            while stack and pos[stack[-1]] > pos[v]:
                last = stack.pop()
            left[v] = last
            if stack:
                right[stack[-1]] = v
            stack.append(v)
        root = stack[0]

        # iterative post-order: compute sizes and the product of binomials
        size = [0] * (n + 1)
        ans = 1
        order = []
        st = [root]
        while st:
            u = st.pop()
            order.append(u)
            if left[u]:
                st.append(left[u])
            if right[u]:
                st.append(right[u])
        for u in reversed(order):
            l, r = left[u], right[u]
            L = size[l] if l else 0
            R = size[r] if r else 0
            size[u] = L + R + 1
            # C(L+R, L)
            ans = ans * fact[L + R] % MOD * inv[L] % MOD * inv[R] % MOD

        return (ans - 1) % MOD
        