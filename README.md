ThreadPool
==========

Porting a simple C++11 [thread pool](https://github.com/progschj/ThreadPool) implementation to Java

Basic usage:
```java
// create thread pool with 4 worker threads
ThreadPool pool = new ThreadPool(4);

// enqueue and store future
Future<Integer> result = pool.enqueue(() -> 42);

// get result from future
System.out.println(result.get());

// close thread pool
pool.close();
```
