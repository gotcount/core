/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.histogram;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
public class LimitedHashHistogramBenchmark extends HistogramBenchmark {

    @Override
    Histogram<Integer> getInstance() {
        return new LimitedHashHistogram<>(20);
    }
    
}
