#Runtime

####_Work and Depth:_

-  Work W(e): number of steps e would take if there was no parallelism
   -  $W(parallel(e_1, e_2)) = W(e_1) + W(e_2) + c_2
-  Depth D(e): number of steps if we had unbounded parallelism
   -  $D(parallel(e_1, e_2)) = max(D(e_1), D(e_2) + c_1

For parts of code where we do not use parallel explicitly, we must add up costs. For function call or operation $f(e_1, ... , e_n)$:

-  $W(f(e_1, ... , e_n)) = W(e_1) + ... + W(e_n) + W(f)(v_1, ..., v_n)$
-  $D(f(e_1, ... , e_n)) = D(e_1) + ... + D(e_n) + D(f)(v_1, ..., v_n)$

Suppose we know $W(e)$ and $D(e)$ and our platform has $P$ parallel threads
Regardless of $P$, cannot finish sooner than $D(e)$ because of dependencies
Regardless of $D(e)$, cannot finish sooner than $\frac{W(e)}{P}$ since every piece of work needs to be done
So it is reasonable to use this estimate for running time:
$ \qquad \qquad D(e) + \frac{W(e)}{P} $
Given $W$ and $D$, we can estimate how programs behave for different $P$

-  If $P$ is constant but inputs grow, parallel programs have same
   asymptotic time complexity as sequential ones
-  Even if we have infinite resources, $(P \xrightarrow[]{} \infty)$, we have non-zero complexity given by $D(e)$

**_Asymptotic analysis of sequential running time:_**

```scala
def sumSegment(a: Array[Int], p: Double, s: Int, t: Int): Int = {
    var i= s; var sum: Int = 0
    while (i < t) {
        sum= sum + power(a(i), p)
        i= i + 1
    }
    sum }
```

$W(s, t) = O(t-s) \text{, a function of the form: }\\ \qquad c_1(t - s) + c_2$

-  t - s loop iterations
-  a constant amount of work in each iteration

**_Analysis of recursive functions:_**

```scala
def segmentRec(a: Array[Int], p: Double, s: Int, t: Int) = {
    if (t - s < threshold)
        sumSegment(a, p, s, t)
    else {
        val m= s + (t - s)/2
        val (sum1, sum2)= (segmentRec(a, p, s, m), segmentRec(a, p, m, t))
        sum1 + sum2 }
    }
```

$W(s, t)\begin{cases}
   c_1(t-s) + c_2 &\text{if } t-s < threshold \\
   W(s, m) + W(m, t) + c_3 &\text{otherwise, for } m= \lfloor \frac{(s+t)}{2}\rfloor
\end{cases}$

######

-  Assume $t - s = 2^N(threshold - 1)$, where N is the depth of the tree

######

-  Computation tree has $2^N$ leaves and $2^N - 1$ internal nodes

######

-  $W(s, t) = 2^N(c1(threshold - 1) + c_2) + (2^N - 1)c_3 \\ \qquad= 2^Nc_4 + c_5$

######

-  $\text{If } \quad 2^N-1 < \frac{t - s}{threshold - 1} \leq 2^N \text{, we have } \\ \qquad W(s, t) \leq 2^Nc_4 + c_5 < (t - s)\frac{2}{(threshold - 1)} + c_5$

######

-  $W(s, t) \text{ is in } O(t - s)$. Sequential segmentRec is linear in $t - s$

###

$D(s, t)\begin{cases}
   c_1(t-s) + c_2 &\text{if } t-s < threshold \\
   max(D(s, m), W(m, t)) + c_3 &\text{otherwise, for } m= \lfloor \frac{(s+t)}{2}\rfloor
\end{cases}$

-  Assume $t - s = 2^N(threshold - 1)$, where N is the depth of the tree

######

-  Computation tree has $2^N$ leaves and $2^N - 1$ internal nodes

######

-  The value of $D(s, t)$ in leaves of computation tree: $c_1(threshold - 1) + c_2$

######

-  One level above: $\qquad c_1(threshold - 1) + c_2 + c_3$

######

-  Solution bounded by $O(N)$. Also, running time is monotonic in $t-s$

######

-  $\text{If } \quad 2^N-1 < \frac{t - s}{threshold - 1} \leq 2^N \text{, we have } \\ \qquad N \leq \log{(t-s)} + c_6$

######

-  $D(s, t) \text{ is in } O(\log{(t-s)})$
