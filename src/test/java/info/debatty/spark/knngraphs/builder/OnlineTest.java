/*
 * The MIT License
 *
 * Copyright 2016 Thibault Debatty.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package info.debatty.spark.knngraphs.builder;

import info.debatty.java.graphs.NeighborList;
import info.debatty.java.graphs.Node;
import info.debatty.java.graphs.SimilarityInterface;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

/**
 *
 * @author Thibault Debatty
 */
public class OnlineTest extends TestCase implements Serializable {
    
    public OnlineTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of addNode method, of class Online.
     * @throws java.lang.Exception
     */
    public void testAddNode() throws Exception {
        System.out.println("addNode");
        
        Logger.getLogger("org").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
        
        // Number of nodes in the initial graph
        int n = 500;
        
        // Number of nodes to add to the graph
        int n_add = 20;
        int partitions = 4;
        
        SimilarityInterface<Double> similarity = new SimilarityInterface<Double>() {
            
            public double similarity(Double value1, Double value2) {
                return 1.0 / (1 + Math.abs(value1 - value2));
            }
        };
        
        System.out.println("Create some random nodes");
        Random rand = new Random();
        List<Node<Double>> data = new ArrayList<Node<Double>>();
        while (data.size() < n) {
            data.add(new Node<Double>(String.valueOf(data.size()), 100.0 + 100.0 * rand.nextGaussian()));
            data.add(new Node<Double>(String.valueOf(data.size()), 150.0 + 100.0 * rand.nextGaussian()));
            data.add(new Node<Double>(String.valueOf(data.size()), 300.0 + 100.0 * rand.nextGaussian()));
        }
        

        // Configure spark instance
        SparkConf conf = new SparkConf();
        conf.setAppName("SparkTest");
        conf.setIfMissing("spark.master", "local[*]");
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        // Parallelize the dataset in Spark
        JavaRDD<Node<Double>> nodes = sc.parallelize(data, partitions);
        
        Brute brute = new Brute();
        brute.setK(10);
        brute.setSimilarity(similarity);
        
        System.out.println("Compute the graph and force execution");
        JavaPairRDD<Node<Double>, NeighborList> graph = brute.computeGraph(nodes);
        System.out.println(graph.first());
        
        System.out.println("Prepare the graph for online processing");
        Online<Double> online_graph = new Online<Double>(10, similarity, sc, graph);
        
        
        System.out.println("Add some nodes...");
        for (int i = 0; i < n_add; i++) {
            Node<Double> new_node = new Node<Double>("", 400.0 * rand.nextDouble());
            System.out.println(new_node);
            online_graph.addNode(new_node);   
        }
        List<Tuple2<Node<Double>, NeighborList>> local_graph = online_graph.getGraph().collect();
        sc.close();
        
        assertEquals(data.size() + n_add, local_graph.size());
        assertEquals(online_graph.getGraph().partitions().size(), partitions);
        
    }
    
}