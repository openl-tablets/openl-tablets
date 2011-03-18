set JAVA_OPTS=-server -Xincgc -XX:NewSize=128m -XX:MaxNewSize=128m -Xms512m -Xmx1024m -XX:MaxPermSize=256M -XX:PermSize=128M -XX:+CMSClassUnloadingEnabled

REM Xincgc - Enable the incremental garbage collector. The incremental garbage collector, which is off by default, will eliminate occasional garbage-collection pauses during program execution. However, it can lead to a roughly 10% decrease in overall GC performance.

REM XX:NewSize — Defines the minimum young generation size. BEA recommends testing your production applications starting with a young generation size of 1/3 the total heap size. Using a larger young generation size causes fewer minor collections to occur but may compromise response time goals by cause longer-running full collections.

REM Xms - initial java heap size

REM Xmx - maximum java heap size

REM XX:PermSize - The section of the heap reserved for the permanent generation holds all of the reflective data for the JVM. This size should be  increased to optimize the performance of applications that dynamically load and unload a lot of classes. Setting this to a value of 128MB eliminates the overhead of increasing this part of the heap.

REM CMSPermGenSweepingEnabled - setting includes the PermGen in a garbage collection run. By default, the PermGen space is never included in garbage   collection (and thus grows without bounds). 

REM CMSClassUnloadingEnabled setting tells the PermGen garbage collection sweep to take action on class objects. By default, class objects get an exemption, even when the PermGen space is being visited during a garabage collection.  http://community.eapps.com/showthread.php?p=537
