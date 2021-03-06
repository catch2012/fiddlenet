/**
 * $Id$ 
 *
 * $LastChangedDate$ 
 * 
 * $LastChangedBy$
 */
package instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;


public class InstanceSet implements Iterable<Instance>
{
    private static Logger log = Logger.getLogger (InstanceSet.class.getName());
    private ArrayList<Instance> __instances;
    private AttributeSet __attrSet;
    private ClassificationSet __classSet;


    private InstanceSet () 
    {
        __instances = new ArrayList<Instance>();
        __classSet = new ClassificationSet ();
        __attrSet = new AttributeSet ();
    }


    public InstanceSet (BufferedReader br, String[] attributeNames) throws IOException
    {
        __instances = new ArrayList<Instance>();
        __classSet = new ClassificationSet ();
        __attrSet = new AttributeSet ();

        // Parse data
        String line;
        while (( line = br.readLine() ) != null )
        {
            StringTokenizer st = new StringTokenizer (line, ",");
            ArrayList<Attribute> attrs = new ArrayList<Attribute>();
            int attrCount = st.countTokens() - 1;
            for (int i = 0; i < attrCount; i++)
            {
                // Create attribute, assigning attribute name if we have it
                attrs.add ( new Attribute ( i < attributeNames.length ? attributeNames[i] : "", st.nextToken() ) );
            }

            addInstance (new Instance (attrs, new Classification (st.nextToken())));
        }
    }


    /**
     * Get classification set
     */
    public ClassificationSet getClassificationSet ()
    {
        return __classSet;
    }


    /**
     * Get instance set
     */
    public AttributeSet getAttributeSet ()
    {
        return __attrSet;
    }


    /**
     * Return number if instances in this set
     */
    public int size ()
    {
        return __instances.size ();
    }


    /**
     * Calculate Information entropy of the instance set wrt to
     * classification label
     */
    public double entropy ()
    {
        double entropy = 0;
        for ( Classification clas : __classSet )
        {
            double probability = (double) __classSet.getOccurence (clas) /  (double) this.size();
            double ei = (probability * Math.log (probability) / Math.log (2));
            entropy = entropy - ei;
        }

        return entropy;
    }


    /**
     * Calculate Information entropy of the instance set wrt to
     * given attribute
     */
    public double entropy (Object key)
    {
        double entropy = 0;
        for ( Object val : __attrSet.getValues (key) )
        {
            Attribute attr = __attrSet.getAttribute (key, val);
            double probability = (double) attr.getRef() /  (double) this.size();
            double ei = (probability * Math.log (probability) / Math.log (2));
            entropy = entropy - ei;
        }

        return entropy;
    }


    /**
     * Calculate Total Correlation (Watanabe 1960)
     */
    public double totalCorrelation ()
    {
        // Sigma(Hxi)
        double maxH = 0;
        double marginalEntropy = 0;
        for ( Object key : __attrSet.getKeys() )
        {
            double entropyX = entropy (key);
            log.finest (String.format ("H(%s) = %f", key.toString(), entropyX));
            marginalEntropy += entropyX;
            if (entropyX > maxH) maxH = entropyX;
        }

        // H(x1,x2,...,xn): count occurence of each instance.
        HashMap<String, Integer> instMap = new HashMap<String, Integer>();
        for ( Instance inst : __instances )
        {
            String hash = inst.hashString();
            if ( instMap.containsKey (hash) )
            {
                instMap.put (hash, instMap.get(hash) + 1);
            }
            else
            {
                instMap.put (hash, new Integer(1));
            }
        }

        // H(x1,x2,...,xn): calculate probabilities and entropy.
        double jointEntropy = 0;
        for ( String hash : instMap.keySet() )
        {
            double probability = (double) instMap.get(hash) / (double) size();
            double ei = (probability * Math.log (probability) / Math.log (2));
            jointEntropy -= ei;
        }

        double totalCorrelation = marginalEntropy - jointEntropy;
        double maxTotalCorrelation = marginalEntropy - maxH;
        log.info (String.format("C = %f; Cmax = %f; C/Cmax = %f",
                                totalCorrelation, 
                                maxTotalCorrelation, 
                                totalCorrelation/maxTotalCorrelation));

        return totalCorrelation;
    }

    /**
     * Create a subset of this instance set where all instances have
     * given value for given key.
     */
    public InstanceSet subset ( Object key, Object val )
    {
        InstanceSet subset = new InstanceSet ();
        for ( Instance inst : __instances )
        {
            if ( inst.getAttribute(key).getValue().equals(val) )
            {
                subset.addInstance (inst);
            }
        }

        return subset;
    }

    /**
     * Create a subset of this instance set where all instances have
     * the given classification.
     */
    public InstanceSet subset ( Classification clas )
    {
        InstanceSet subset = new InstanceSet ();
        for ( Instance inst : __instances )
        {
            if ( inst.getClassification().equals (clas) )
            {
                subset.addInstance (inst);
            }
        }

        return subset;
    }


    /**
     * Deep copy given instance and add to set.
     */
    public void addInstance (Instance inst)
    {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>();
        for ( Attribute attr : inst )
        {
            attrs.add ( __attrSet.addAttribute ( attr.getKey(), attr.getValue() ) );
        }

        Classification c = __classSet.getClassification (inst.getClassification().getValue());
        __instances.add (new Instance (attrs, c));
        log.finest ("Added instance: " + inst.toString());
    }


    /**
     * Return value domain for given attribute.
     */
    public Set<Object> getValues (Object key)
    {
        return __attrSet.getValues (key);
    }


    /**
     * Fold this instance set.
     */
    public List<Fold> fold (int K)
    {
        ArrayList<Fold> folds = new ArrayList<Fold>();
        ArrayList<Instance> shuffledInstances = (ArrayList<Instance>) __instances.clone ();
        Collections.shuffle (shuffledInstances);
        K = Math.max (K, 1);
        int testSize = size()/K;

        for (int n = 0; n < K; n++)
        {
            InstanceSet testSet = new InstanceSet ();
            InstanceSet trainSet = new InstanceSet ();

            // Create test set: take element range ((size/K)n, (size/K)(n+1)-1)
            int startTestIndex = (testSize*n);
            int endTestIndex = (testSize*(n+1));

            log.fine (String.format ("fold(): creating fold " + n));
            for (int i = 0; i < startTestIndex; i++)
            {
                log.finest (String.format ("fold(): adding instance %d to training set %d", i, n));
                trainSet.addInstance (shuffledInstances.get(i));
            }
            for (int i = startTestIndex ; i < endTestIndex; i++)
            {
                log.finest (String.format ("fold(): adding instance %d to test set %d", i, n));
                testSet.addInstance (shuffledInstances.get(i));
            }
            for (int i = endTestIndex; i < size(); i++)
            {
                log.finest (String.format ("fold(): adding instance %d to training set %d", i, n));
                trainSet.addInstance (shuffledInstances.get(i));
            }
            
            folds.add (new Fold (n, trainSet, testSet));
        }
        return folds;
    }


    public Iterator<Instance> iterator ()
    {
        return __instances.iterator ();
    }


    public String toString ()
    {
        StringBuffer sb = new StringBuffer ();
        sb.append ( String.format ("[size=%d][entropy=%f][classificationSet=[%s]][attributes=[", 
                                   size(), entropy(), __classSet) );
        for ( Object key : __attrSet.getKeys() )
        {
            sb.append (String.format("[%s]", key.toString()));
        }
        sb.append ("]]");
        return sb.toString ();
    }
}
