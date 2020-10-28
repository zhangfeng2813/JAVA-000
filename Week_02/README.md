from https://github.com/catberus/JAVA-000/tree/main/Week_02

# GC实战

## 1. 模拟生成对象

#### 代码

```java

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
/*
演示GC日志生成与解读
*/
public class GCLogAnalysis {
    private static Random random = new Random();
    public static void main(String[] args) {
        // 当前毫秒时间戳
        long startMillis = System.currentTimeMillis();
        // 持续运行毫秒数; 可根据需要进行修改
        long timeoutMillis = TimeUnit.SECONDS.toMillis(1);
        // 结束时间戳
        long endMillis = startMillis + timeoutMillis;
        LongAdder counter = new LongAdder();
        System.out.println("正在执行...");
        // 缓存一部分对象; 进入老年代
        int cacheSize = 2000;
        Object[] cachedGarbage = new Object[cacheSize];
        // 在此时间范围内,持续循环
        while (System.currentTimeMillis() < endMillis) {
            // 生成垃圾对象
            Object garbage = generateGarbage(100*1024);
            counter.increment();
            int randomIndex = random.nextInt(2 * cacheSize);
            if (randomIndex < cacheSize) {
                cachedGarbage[randomIndex] = garbage;
            }
        }
        System.out.println("执行结束!共生成对象次数:" + counter.longValue());
    }

    // 生成对象
    private static Object generateGarbage(int max) {
        int randomSize = random.nextInt(max);
        int type = randomSize % 4;
        Object result = null;
        switch (type) {
            case 0:
                result = new int[randomSize];
                break;
            case 1:
                result = new byte[randomSize];
                break;
            case 2:
                result = new double[randomSize];
                break;
            default:
                StringBuilder builder = new StringBuilder();
                String randomString = "randomString-Anything";
                while (builder.length() < randomSize) {
                    builder.append(randomString);
                    builder.append(max);
                    builder.append(randomSize);
                }
                result = builder.toString();
                break;
        }
        return result;
    }
}

```

#### GC选择

| 参数                    | 新生代GC          | 老年代GC     |
| ----------------------- | ----------------- | ------------ |
| -XX:+UseSerialGC        | Serial            | SerialOld    |
| -XX:+UseParallelGC      | Parallel Scavenge | Parallel Old |
| -XX:+UseConcMarkSweepGC | Parallel          | CMS          |
| -XX:+UseG1GC            | -                 | -            |



 #### 运行结果

1. SerialGC

| heap  | 生成对象数 | GC时间(ms) | GC次数 | YoungGC时间(ms) | YoungGC次数 | FullGC时间(ms) | FullGC次数 |
| ----- | ---------- | ---------- | ------ | --------------- | ----------- | -------------- | ---------- |
| 256M  | 2715       | 498        | 12     | 284             | 9           | 214            | 3          |
| 521M  | 2819       | 366        | 5      | 366             | 5           | -              | -          |
| 2048M | 2101       | 221        | 1      | 221             | 1           | -              | -          |
| 4096M | 1930       | -          | -      | -               | -           | -              | -          |

2. ParallelGC

| heap  | 生成对象数 | GC时间(ms) | GC次数 | YoungGC时间(ms) | YoungGC次数 | FullGC时间(ms) | FullGC次数 |
| ----- | ---------- | ---------- | ------ | --------------- | ----------- | -------------- | ---------- |
| 256M  | 2670       | 444.912    | 18     | 135.057         | 11          | 309.854        | 7          |
| 521M  | 4260       | 289.743    | 12     | 239.964         | 11          | 49.78          | 1          |
| 2048M | 1871       | 82.641     | 1      | 82.641          | 1           | -              | -          |
| 4096M | 2192       | -          | -      | -               | -           | -              | -          |

3. CMS

| heap  | 生成对象数 | GC时间(ms) | GC次数 | YoungGC时间(ms) | YoungGC次数 | FullGC时间(ms) | FullGC次数 |
| ----- | ---------- | ---------- | ------ | --------------- | ----------- | -------------- | ---------- |
| 256M  | 2940       | 657.258    | 42     | 255.235         | 11          | 402.023        | 31         |
| 521M  | 3501       | 399.606    | 12     | 364.244         | 6           | 35.362         | 6          |
| 2048M | 2067       | 77.351     | 1      | 77.351          | 1           | -              | --         |
| 4096M | 2015       | 81.409     | 1      | 81.409          | 1           | -              | --         |

4. G1

| heap  | 生成对象数 | GC时间(ms) | GC次数 | YoungGC时间(ms) | YoungGC次数 | FullGC时间(ms) | FullGC次数 |
| ----- | ---------- | ---------- | ------ | --------------- | ----------- | -------------- | ---------- |
| 256M  | -          | -          | -      | -               | -           | -              | -          |
| 521M  | -          | -          | -      | -               | -           | -              | --         |
| 2048M | 4297       | 164.599    | 8      | -               | -           | -              | -          |
| 4096M | 4628       | 192.468    | 7      | -               | -           | -              | -          |

#### 结论

- 并行GC平均单次GC时间小于串行GC，效率更高
- CMS单次GC时间远小于其他GC
- 随着堆内存的增大，G1效率提升明显   

####	问题

- 随着堆内存的增大，GC总时间减少，而Serial、Parallel、CMS都呈现了一定程度额的性能下降，原因是什么？



## 2. web工程压测

#### 压测命令

```powershell
sb -u http://localhost:8088/api/hello -N 60 -c 10
```

#### JVM参数

```
-Xmx30m -Xms30m -Xmn13m
```

#### 运行结果

| GC       | RPS    | 90% below | 95% below | 99% below | avg  |
| -------- | ------ | --------- | --------- | --------- | ---- |
| Serial   | 3625.4 | 0         | 1         | 16        | 0.5  |
| Parallel | 3732.5 | 0         | 1         | 14        | 0.5  |
| CMS      | 3629.6 | 0         | 1         | 11        | 0.4  |

#### 结论

尽管调整了堆内存大小，但因老年代增长较慢，更换GC效果似乎并不明显



## 3. 总结

#### Serial

- 单线程，进行垃圾收集时必须暂停其他工作线程
- 虚拟机在Client模式下的默认新生代收集器

#### ParNew

- Serial多线程版本，随CPU数量增加
- CMS GC的默认新生代收集器

#### Parallel Scavenge

- 并行GC，吞吐量优先

- Server模式默认新生代收集器
- 与ParNew相比，具有自适应调节策略（GC Ergonomics）

---

#### Serial Old

- 单线程，采用**标记-整理**算法
- 虚拟机在Client模式下的默认老年代收集器

#### Parallel Old

- 多线程，采用**标记-整理**算法
- Server模式默认老年代收集器

#### CMS

- 并发收集，以获取最短回收停顿时间为目标，采用**标记-清除**算法
- 无法清理浮动垃圾，可能产生碎片

---

#### G1

- 管理整个GC堆，整体基于**标记-整理**算法，局部基于**复制**算法，不会产生内存碎片
- 新生代与老年代不再物理隔离，将Java堆分为多个独立区域，根据各区域垃圾堆积价值大小优先收集。
- 可预测停顿时间



















