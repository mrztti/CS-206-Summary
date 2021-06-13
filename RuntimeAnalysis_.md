# Runtime
  
  
#### _Work and Depth:_
  
  
-  Work W(e): number of steps e would take if there was no parallelism
   -  <img src="https://latex.codecogs.com/gif.latex?W(parallel(e_1,%20e_2))%20=%20W(e_1)%20+%20W(e_2)%20+%20c_2-%20%20Depth%20D(e):%20number%20of%20steps%20if%20we%20had%20unbounded%20parallelism%20%20%20-"/>D(parallel(e_1, e_2)) = max(D(e_1), D(e_2) + c_1
  
For parts of code where we do not use parallel explicitly, we must add up costs. For function call or operation <img src="https://latex.codecogs.com/gif.latex?f(e_1,%20...%20,%20e_n)"/>:
  
-  <img src="https://latex.codecogs.com/gif.latex?W(f(e_1,%20...%20,%20e_n))%20=%20W(e_1)%20+%20...%20+%20W(e_n)%20+%20W(f)(v_1,%20...,%20v_n)"/>
-  <img src="https://latex.codecogs.com/gif.latex?D(f(e_1,%20...%20,%20e_n))%20=%20D(e_1)%20+%20...%20+%20D(e_n)%20+%20D(f)(v_1,%20...,%20v_n)"/>
  
Suppose we know <img src="https://latex.codecogs.com/gif.latex?W(e)"/> and <img src="https://latex.codecogs.com/gif.latex?D(e)"/> and our platform has <img src="https://latex.codecogs.com/gif.latex?P"/> parallel threads
Regardless of <img src="https://latex.codecogs.com/gif.latex?P"/>, cannot finish sooner than <img src="https://latex.codecogs.com/gif.latex?D(e)"/> because of dependencies
Regardless of <img src="https://latex.codecogs.com/gif.latex?D(e)"/>, cannot finish sooner than <img src="https://latex.codecogs.com/gif.latex?&#x5C;frac{W(e)}{P}"/> since every piece of work needs to be done
So it is reasonable to use this estimate for running time:
<img src="https://latex.codecogs.com/gif.latex?&#x5C;qquad%20&#x5C;qquad%20D(e)%20+%20&#x5C;frac{W(e)}{P}"/>
Given <img src="https://latex.codecogs.com/gif.latex?W"/> and <img src="https://latex.codecogs.com/gif.latex?D"/>, we can estimate how programs behave for different <img src="https://latex.codecogs.com/gif.latex?P"/>
  
-  If <img src="https://latex.codecogs.com/gif.latex?P"/> is constant but inputs grow, parallel programs have same
   asymptotic time complexity as sequential ones
-  Even if we have infinite resources, <img src="https://latex.codecogs.com/gif.latex?(P%20&#x5C;xrightarrow[]{}%20&#x5C;infty)"/>, we have non-zero complexity given by <img src="https://latex.codecogs.com/gif.latex?D(e)"/>
  
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
  
<img src="https://latex.codecogs.com/gif.latex?W(s,%20t)%20=%20O(t-s)%20&#x5C;text{,%20a%20function%20of%20the%20form:%20}&#x5C;&#x5C;%20&#x5C;qquad%20c_1(t%20-%20s)%20+%20c_2"/>
  
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
  
<img src="https://latex.codecogs.com/gif.latex?W(s,%20t)&#x5C;begin{cases}%20%20%20c_1(t-s)%20+%20c_2%20&amp;&#x5C;text{if%20}%20t-s%20&lt;%20threshold%20&#x5C;&#x5C;%20%20%20W(s,%20m)%20+%20W(m,%20t)%20+%20c_3%20&amp;&#x5C;text{otherwise,%20for%20}%20m=%20&#x5C;lfloor%20&#x5C;frac{(s+t)}{2}&#x5C;rfloor&#x5C;end{cases}"/>
  
###### 
  
  
-  Assume <img src="https://latex.codecogs.com/gif.latex?t%20-%20s%20=%202^N(threshold%20-%201)"/>, where N is the depth of the tree
  
###### 
  
  
-  Computation tree has <img src="https://latex.codecogs.com/gif.latex?2^N"/> leaves and <img src="https://latex.codecogs.com/gif.latex?2^N%20-%201"/> internal nodes
  
###### 
  
  
-  <img src="https://latex.codecogs.com/gif.latex?W(s,%20t)%20=%202^N(c1(threshold%20-%201)%20+%20c_2)%20+%20(2^N%20-%201)c_3%20&#x5C;&#x5C;%20&#x5C;qquad=%202^Nc_4%20+%20c_5"/>
  
###### 
  
  
-  <img src="https://latex.codecogs.com/gif.latex?&#x5C;text{If%20}%20&#x5C;quad%202^N-1%20&lt;%20&#x5C;frac{t%20-%20s}{threshold%20-%201}%20&#x5C;leq%202^N%20&#x5C;text{,%20we%20have%20}%20&#x5C;&#x5C;%20&#x5C;qquad%20W(s,%20t)%20&#x5C;leq%202^Nc_4%20+%20c_5%20&lt;%20(t%20-%20s)&#x5C;frac{2}{(threshold%20-%201)}%20+%20c_5"/>
  
###### 
  
  
-  <img src="https://latex.codecogs.com/gif.latex?W(s,%20t)%20&#x5C;text{%20is%20in%20}%20O(t%20-%20s)"/>. Sequential segmentRec is linear in <img src="https://latex.codecogs.com/gif.latex?t%20-%20s"/>
  
### 
  
  
<img src="https://latex.codecogs.com/gif.latex?D(s,%20t)&#x5C;begin{cases}%20%20%20c_1(t-s)%20+%20c_2%20&amp;&#x5C;text{if%20}%20t-s%20&lt;%20threshold%20&#x5C;&#x5C;%20%20%20max(D(s,%20m),%20W(m,%20t))%20+%20c_3%20&amp;&#x5C;text{otherwise,%20for%20}%20m=%20&#x5C;lfloor%20&#x5C;frac{(s+t)}{2}&#x5C;rfloor&#x5C;end{cases}"/>
  
-  Assume <img src="https://latex.codecogs.com/gif.latex?t%20-%20s%20=%202^N(threshold%20-%201)"/>, where N is the depth of the tree
  
###### 
  
  
-  Computation tree has <img src="https://latex.codecogs.com/gif.latex?2^N"/> leaves and <img src="https://latex.codecogs.com/gif.latex?2^N%20-%201"/> internal nodes
  
###### 
  
  
-  The value of <img src="https://latex.codecogs.com/gif.latex?D(s,%20t)"/> in leaves of computation tree: <img src="https://latex.codecogs.com/gif.latex?c_1(threshold%20-%201)%20+%20c_2"/>
  
###### 
  
  
-  One level above: <img src="https://latex.codecogs.com/gif.latex?&#x5C;qquad%20c_1(threshold%20-%201)%20+%20c_2%20+%20c_3"/>
  
###### 
  
  
-  Solution bounded by <img src="https://latex.codecogs.com/gif.latex?O(N)"/>. Also, running time is monotonic in <img src="https://latex.codecogs.com/gif.latex?t-s"/>
  
###### 
  
  
-  <img src="https://latex.codecogs.com/gif.latex?&#x5C;text{If%20}%20&#x5C;quad%202^N-1%20&lt;%20&#x5C;frac{t%20-%20s}{threshold%20-%201}%20&#x5C;leq%202^N%20&#x5C;text{,%20we%20have%20}%20&#x5C;&#x5C;%20&#x5C;qquad%20N%20&#x5C;leq%20&#x5C;log{(t-s)}%20+%20c_6"/>
  
###### 
  
  
-  <img src="https://latex.codecogs.com/gif.latex?D(s,%20t)%20&#x5C;text{%20is%20in%20}%20O(&#x5C;log{(t-s)})"/>
  