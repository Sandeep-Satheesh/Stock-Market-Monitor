package utils;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.LinkedList;

public class TimeSeriesPlotter {
    private SwingWrapper<XYChart> sw;
    private XYChart chart;
    private MySwingWorker worker;
    private Observable<Pair<Long, Double>> dataSource;
    private Disposable subscription;
    private JFrame chartFrame;

    private String graphTitle, xTitle, yTitle, seriesName;
    private final LinkedList<Double> fifoY;
    private final LinkedList<Long> fifoX;
    private volatile boolean dataReady = false;

    public TimeSeriesPlotter (Observable<Pair<Long, Double>> dataSource, String graphTitle, String xTitle, String yTitle, String seriesName) {
        this.graphTitle = graphTitle;
        this.xTitle = xTitle;
        this.yTitle = yTitle;
        this.seriesName = seriesName;
        this.dataSource = dataSource;

        fifoX = new LinkedList<>();
        fifoY = new LinkedList<>();
        worker = new MySwingWorker();
        worker.execute();
    }

    private void initPlotter() {
        chart = new XYChartBuilder().width(800)
                .height(600)
                .title(graphTitle)
                .xAxisTitle(xTitle)
                .yAxisTitle(yTitle)
                .build();

        //QuickChart.getChart(graphTitle, xTitle, yTitle, seriesName, new double[]{System.currentTimeMillis()}, new double[]{0});
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setXAxisTicksVisible(true);
        chart.getStyler().setXAxisLabelRotation(60);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setCursorEnabled(true);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Area);

        chart.setCustomXAxisTickLabelsFormatter(aDouble -> new SimpleDateFormat("HH:mm:ss").format(aDouble));
        sw = new SwingWrapper<>(chart);
        sw.setTitle(graphTitle);
        chartFrame = sw.displayChart();
        chartFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        chartFrame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                JOptionPane.showMessageDialog(null, "Press the 'Stop Monitor' button to stop monitoring!");

            }
        });
    }

    public void pushDataPoint(Long xDataPoint, Double yDataPoint) {
        synchronized (fifoY) {
            if (fifoY.size() > 20) {
                fifoY.removeFirst();
            }
            fifoY.add(yDataPoint);
        }
        synchronized (fifoX) {
            if (fifoX.size() > 20) {
                fifoX.removeFirst();
            }
            fifoX.add(xDataPoint);

        }
    }

    private void redrawGraph() {
        synchronized (fifoY) {
            synchronized (fifoX) {
                double[] yData = new double[fifoY.size()];
                double[] xData = new double[fifoX.size()];

                for (int i = 0; i < yData.length; i++) {
                    yData[i] = fifoY.get(i);
                    xData[i] = fifoX.get(i);
                }
                if (chart.getSeriesMap().containsKey(seriesName))
                    chart.updateXYSeries(seriesName, xData, yData, null);
                else
                    chart.addSeries(seriesName, xData, yData);
            }
        }
        sw.repaintChart();
    }

    public void stopPlotting() {
        if (!subscription.isDisposed())
            subscription.dispose();

        worker.cancel(true);
        chartFrame.dispose();
    }

    private class MySwingWorker extends SwingWorker<Boolean, double[]> {
        @Override
        protected Boolean doInBackground() throws Exception {
            initPlotter();
            subscription = dataSource.subscribe(e -> {
                pushDataPoint(e.a, e.b);
                dataReady = true;
            });

            while (!isCancelled()) {
                if (dataReady) {
                    redrawGraph();
                    dataReady = false;
                }
            }
            if (!subscription.isDisposed()) subscription.dispose();
            return true;
        }
    }
}
