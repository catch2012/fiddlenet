/**
 * $Id$ 
 *
 * $LastChangedDate$ 
 * 
 * $LastChangedBy$
 */


import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import aho.*;
import util.*;


public class AhoIndexMain
{
    private static Logger log = Logger.getLogger (AhoIndexMain.class.getName());
    private String __keywordsFile = "data/keywords";
    private String __documentDirectory = "data/documents";
    private int __poolSize = 1; 

    Options __opt;
    CommandLine __cl;


    private AhoIndexMain ()
    {
        __opt = new Options(); 
        __opt.addOption("h", false, "Print help");
        __opt.addOption("k", true, "keywords file");
        __opt.addOption("d", true, "document directory");
        __opt.addOption("g", false, "Generate DOT visulaization of state machine");
        __opt.addOption("p", true, "Thread pool size");
    }

     
    public static void main (String[] args)
    {
        new AhoIndexMain().run (args);
    }

    
    private void printUsage (String message, int rc)
    {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp (message, __opt);
        System.exit (rc);
    }

    
    private void run (String[] args)
    {
        try
        {        
            __cl = (new BasicParser()).parse (__opt, args); 
            if ( __cl.hasOption ('h') ) printUsage ("help", 0);
            if ( __cl.hasOption ('k') ) __keywordsFile = __cl.getOptionValue ('k');  
            if ( __cl.hasOption ('d') ) __documentDirectory = __cl.getOptionValue ('d');
            if ( __cl.hasOption ('p') ) __poolSize = Integer.parseInt(__cl.getOptionValue ('p'));
        }
        catch (ParseException ex)
        {
            printUsage (ex.getMessage(), 1);
            System.exit (1);
        }

        // Read data
        Data data = new Data (__keywordsFile, __documentDirectory);
        Timer timer = new Timer ();

        // Create state machine
        log.info ("Creating state machine...");
        // Start execution timer
        timer.reset();
        timer.start();
        StateMachine stateMachine = new StateMachine (data.getKeywords());
        timer.stop ();
        log.info ("Created state machine for " + data.getKeywords().size() 
                  + " keywords in " + timer.duration() + " miliseconds.");

        // Output DOT
        if ( __cl.hasOption ('g') )
        {
            System.out.println (stateMachine.dot());
        }
        
        // Index using thread pool
        log.info ("Creating thread pool with " + __poolSize + " threads");
        ExecutorService pool = Executors.newFixedThreadPool(__poolSize);
        List<Match> matches = Collections.synchronizedList (new ArrayList<Match>());
        timer.reset ();
        timer.start ();

        for (Document document : data.getDocuments())
        {
            AhoIndexer indexer = new AhoIndexer (stateMachine, document, matches);
            pool.execute (indexer);            
        }

        try
        {
            pool.shutdown ();
            pool.awaitTermination (3600, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex)
        {
            log.severe ("Thread pool shutdown interrupted: " + ex);
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        timer.stop ();
        log.fine (String.format("Indexed %d of %d documents using %d keywords in %d miliseconds.",
                                matches.size() ,
                                data.getDocuments().size(),
                                data.getKeywords().size(),
                                timer.duration()));
    }
}




