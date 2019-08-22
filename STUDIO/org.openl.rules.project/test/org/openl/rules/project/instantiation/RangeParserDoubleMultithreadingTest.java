package org.openl.rules.project.instantiation;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

public class RangeParserDoubleMultithreadingTest {

    // EPBDS-8021. Just for mvn multithreading simulation, for example 7 threads
    // TODO should consider migration to the JUnit5 or TestNG for concurrency running
    private int numsOfThreads = 7;

    private int countOfRuns = 50;

    @Test
    public void doubleRangeMultithreadingRun() throws Exception {
        Runnable t1 = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < countOfRuns; i++) {
                    try {
                        rangeParserDoubleWithBracket();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread[] treads = new Thread[numsOfThreads];
        for (int i = 0; i < treads.length; i++) {
            treads[i] = new Thread(t1);
            treads[i].start();
        }

        for (int i = 0; i < treads.length; i++) {
            treads[i].join();
        }
    }

    private void rangeParserDoubleWithBracket() throws Exception {
        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<DoubleRange> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
        SimpleProjectEngineFactory<DoubleRange> factory = builder.setProject("test-resources/concurrency-test")
            .setInterfaceClass(DoubleRange.class)
            .build();

        DoubleRange instance = factory.newInstance();

        assertEquals(1.0, instance.CupCapAdj(-749));
        assertEquals(2.0, instance.CupCapAdj(-750));
        assertEquals(3.0, instance.CupCapAdj(-751));
        assertEquals(4.0, instance.CupCapAdj(-752));
        assertEquals(5.0, instance.CupCapAdj(-753));
        assertEquals(6.0, instance.CupCapAdj(-754));
        assertEquals(7.0, instance.CupCapAdj(-755));
        assertEquals(8.0, instance.CupCapAdj(-756));
        assertEquals(9.0, instance.CupCapAdj(-757));
        assertEquals(10.0, instance.CupCapAdj(-758));
        assertEquals(11.0, instance.CupCapAdj(-759));
        assertEquals(12.0, instance.CupCapAdj(-760));
        assertEquals(13.0, instance.CupCapAdj(-761));
        assertEquals(14.0, instance.CupCapAdj(-762));
        assertEquals(15.0, instance.CupCapAdj(-763));
        assertEquals(16.0, instance.CupCapAdj(-764));
        assertEquals(17.0, instance.CupCapAdj(-765));
        assertEquals(18.0, instance.CupCapAdj(-766));
        assertEquals(19.0, instance.CupCapAdj(-767));
        assertEquals(20.0, instance.CupCapAdj(-768));
        assertEquals(21.0, instance.CupCapAdj(-769));
        assertEquals(22.0, instance.CupCapAdj(-770));
        assertEquals(23.0, instance.CupCapAdj(-771));
        assertEquals(24.0, instance.CupCapAdj(-772));
        assertEquals(25.0, instance.CupCapAdj(-773));
        assertEquals(26.0, instance.CupCapAdj(-774));
        assertEquals(27.0, instance.CupCapAdj(-775));
        assertEquals(28.0, instance.CupCapAdj(-776));
        assertEquals(29.0, instance.CupCapAdj(-777));
        assertEquals(30.0, instance.CupCapAdj(-778));
        assertEquals(31.0, instance.CupCapAdj(-779));
        assertEquals(32.0, instance.CupCapAdj(-780));
        assertEquals(33.0, instance.CupCapAdj(-781));
        assertEquals(34.0, instance.CupCapAdj(-782));
        assertEquals(35.0, instance.CupCapAdj(-783));
        assertEquals(36.0, instance.CupCapAdj(-784));
        assertEquals(37.0, instance.CupCapAdj(-785));
        assertEquals(38.0, instance.CupCapAdj(-786));
        assertEquals(39.0, instance.CupCapAdj(-787));
        assertEquals(40.0, instance.CupCapAdj(-788));
        assertEquals(41.0, instance.CupCapAdj(-789));
        assertEquals(42.0, instance.CupCapAdj(-790));
        assertEquals(43.0, instance.CupCapAdj(-791));
        assertEquals(44.0, instance.CupCapAdj(-792));
        assertEquals(45.0, instance.CupCapAdj(-793));
        assertEquals(46.0, instance.CupCapAdj(-794));
        assertEquals(47.0, instance.CupCapAdj(-795));
        assertEquals(48.0, instance.CupCapAdj(-796));
        assertEquals(49.0, instance.CupCapAdj(-797));
        assertEquals(50.0, instance.CupCapAdj(-798));
        assertEquals(51.0, instance.CupCapAdj(-799));
        assertEquals(52.0, instance.CupCapAdj(-800));
        assertEquals(53.0, instance.CupCapAdj(-801));
        assertEquals(54.0, instance.CupCapAdj(-802));
        assertEquals(55.0, instance.CupCapAdj(-803));
        assertEquals(56.0, instance.CupCapAdj(-804));
        assertEquals(57.0, instance.CupCapAdj(-805));
        assertEquals(58.0, instance.CupCapAdj(-806));
        assertEquals(59.0, instance.CupCapAdj(-807));
        assertEquals(60.0, instance.CupCapAdj(-808));
        assertEquals(61.0, instance.CupCapAdj(-809));
        assertEquals(62.0, instance.CupCapAdj(-810));
        assertEquals(63.0, instance.CupCapAdj(-811));
        assertEquals(64.0, instance.CupCapAdj(-812));
        assertEquals(65.0, instance.CupCapAdj(-813));
        assertEquals(66.0, instance.CupCapAdj(-814));
        assertEquals(67.0, instance.CupCapAdj(-815));
        assertEquals(68.0, instance.CupCapAdj(-816));
        assertEquals(69.0, instance.CupCapAdj(-817));
        assertEquals(70.0, instance.CupCapAdj(-818));
        assertEquals(71.0, instance.CupCapAdj(-819));
        assertEquals(72.0, instance.CupCapAdj(-820));
        assertEquals(73.0, instance.CupCapAdj(-821));
        assertEquals(74.0, instance.CupCapAdj(-822));
        assertEquals(75.0, instance.CupCapAdj(-823));
        assertEquals(76.0, instance.CupCapAdj(-824));
        assertEquals(77.0, instance.CupCapAdj(-825));
        assertEquals(78.0, instance.CupCapAdj(-826));
        assertEquals(79.0, instance.CupCapAdj(-827));
        assertEquals(80.0, instance.CupCapAdj(-828));
        assertEquals(81.0, instance.CupCapAdj(-829));
        assertEquals(82.0, instance.CupCapAdj(-830));
        assertEquals(83.0, instance.CupCapAdj(-831));
        assertEquals(84.0, instance.CupCapAdj(-832));
        assertEquals(85.0, instance.CupCapAdj(-833));
        assertEquals(86.0, instance.CupCapAdj(-834));
        assertEquals(87.0, instance.CupCapAdj(-835));
        assertEquals(88.0, instance.CupCapAdj(-836));
        assertEquals(89.0, instance.CupCapAdj(-837));
        assertEquals(90.0, instance.CupCapAdj(-838));
        assertEquals(91.0, instance.CupCapAdj(-839));
        assertEquals(92.0, instance.CupCapAdj(-840));
        assertEquals(93.0, instance.CupCapAdj(-841));
        assertEquals(94.0, instance.CupCapAdj(-842));
        assertEquals(95.0, instance.CupCapAdj(-843));
        assertEquals(96.0, instance.CupCapAdj(-844));
        assertEquals(97.0, instance.CupCapAdj(-845));
        assertEquals(98.0, instance.CupCapAdj(-846));
        assertEquals(99.0, instance.CupCapAdj(-847));
        assertEquals(100.0, instance.CupCapAdj(-848));
        assertEquals(101.0, instance.CupCapAdj(-849));
        assertEquals(102.0, instance.CupCapAdj(-850));
        assertEquals(103.0, instance.CupCapAdj(-851));
        assertEquals(104.0, instance.CupCapAdj(-852));
        assertEquals(105.0, instance.CupCapAdj(-853));
        assertEquals(106.0, instance.CupCapAdj(-854));
        assertEquals(107.0, instance.CupCapAdj(-855));
        assertEquals(108.0, instance.CupCapAdj(-856));
        assertEquals(109.0, instance.CupCapAdj(-857));
        assertEquals(110.0, instance.CupCapAdj(-858));
        assertEquals(111.0, instance.CupCapAdj(-859));
        assertEquals(112.0, instance.CupCapAdj(-860));
        assertEquals(113.0, instance.CupCapAdj(-861));
        assertEquals(114.0, instance.CupCapAdj(-862));
        assertEquals(115.0, instance.CupCapAdj(-863));
        assertEquals(116.0, instance.CupCapAdj(-864));
        assertEquals(117.0, instance.CupCapAdj(-865));
        assertEquals(118.0, instance.CupCapAdj(-866));
        assertEquals(119.0, instance.CupCapAdj(-867));
        assertEquals(120.0, instance.CupCapAdj(-868));
        assertEquals(121.0, instance.CupCapAdj(-869));
        assertEquals(122.0, instance.CupCapAdj(-870));
        assertEquals(123.0, instance.CupCapAdj(-871));
        assertEquals(124.0, instance.CupCapAdj(-872));
        assertEquals(125.0, instance.CupCapAdj(-873));
        assertEquals(126.0, instance.CupCapAdj(-874));
        assertEquals(127.0, instance.CupCapAdj(-875));
        assertEquals(128.0, instance.CupCapAdj(-876));
        assertEquals(129.0, instance.CupCapAdj(-877));
        assertEquals(130.0, instance.CupCapAdj(-878));
        assertEquals(131.0, instance.CupCapAdj(-879));
        assertEquals(132.0, instance.CupCapAdj(-880));
        assertEquals(133.0, instance.CupCapAdj(-881));
        assertEquals(134.0, instance.CupCapAdj(-882));
        assertEquals(135.0, instance.CupCapAdj(-883));
        assertEquals(136.0, instance.CupCapAdj(-884));
        assertEquals(137.0, instance.CupCapAdj(-885));
        assertEquals(138.0, instance.CupCapAdj(-886));
        assertEquals(139.0, instance.CupCapAdj(-887));
        assertEquals(140.0, instance.CupCapAdj(-888));
        assertEquals(141.0, instance.CupCapAdj(-889));
        assertEquals(142.0, instance.CupCapAdj(-890));
        assertEquals(143.0, instance.CupCapAdj(-891));
        assertEquals(144.0, instance.CupCapAdj(-892));
        assertEquals(145.0, instance.CupCapAdj(-893));
        assertEquals(146.0, instance.CupCapAdj(-894));
        assertEquals(147.0, instance.CupCapAdj(-895));
        assertEquals(148.0, instance.CupCapAdj(-896));
        assertEquals(149.0, instance.CupCapAdj(-897));
        assertEquals(150.0, instance.CupCapAdj(-898));
        assertEquals(151.0, instance.CupCapAdj(-899));
        assertEquals(152.0, instance.CupCapAdj(-900));
        assertEquals(153.0, instance.CupCapAdj(-901));
        assertEquals(154.0, instance.CupCapAdj(-902));
    }

    private interface DoubleRange {
        Double CupCapAdj(double expPremDDIFF);
    }
}
