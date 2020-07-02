# Mini Spring
基于Java Servlet实现Spring基本框架。
Spring的三个阶段，配置阶段、初始化阶段和运行阶段。
- 配置阶段：主要是完成application.xml配置和Annotation配置。
- 初始化阶段：主要是加载并解析配置信息，然后，初始化IOC容器，完成容器的DI操作，已经完成HandlerMapping的初始化。
- 运行阶段：主要是完成Spring容器启动以后，完成用户请求的内部调度，并返回响应结果。

## Demo
~~~
mvn clean jetty:run -DskipTests
~~~
- http://localhost:8080/demo/add.json?a=1&b=3
- http://localhost:8080/demo/query.json?name=Merlin
- http://localhost:8080/demo/remove.json?id=6

## IDEA下使用Jetty进行Debug模式调试
步骤如下：
（1）找到选项卡中的 –Run– 然后找到 –Edit Configurations
（2）点击下图中绿色的plus–找到Maven点进去
（3）按照下边的方式在Command line和Profiles中填入下边固定的值，然后在Working Directory中填入自己项目的路径
命令为：clean jetty:run -DskipTests
名字随意，这里叫做JettyDebug
（4）还可以按照相同的方式，设置mvn clean install和mvn jetty:run这两个命令
（5）这样的话，我们就有三个“快捷键”了
（6）在进行调试的时候，只要选择刚才（3）中创建的JettyDebug
然后再点击后边的绿色三角按钮，打好断点之后 运行就可以进入断点进行调试。

## Ref
- [手写Spring](https://gper.club/articles/7e7e7f7ff0g52gce)
- [IDEA下使用Jetty进行Debug模式调试](https://blog.csdn.net/xlgen157387/article/details/47616841)
