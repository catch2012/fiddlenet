/**
 * $Id$ 
 *
 * $LastChangedDate$ 
 * 
 * $LastChangedBy$
 */
package aho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;


public class State implements Iterable<State>
{
    private static Logger log = Logger.getLogger (State.class.getName());
    private int __id = 0;
    private HashMap<Character, State> __transitions = new HashMap<Character, State> ();
    private HashSet<String> __output = new HashSet<String>();
    private State __failure;


    private State () {}
    public State (int id)
    {
        __id = id;
    }

    
    public int getId ()
    {
        return __id;
    }


    public HashMap<Character, State> getTransitions ()
    {
        return __transitions;
    }


    /**
     * Create a new state and add a transition to it from this state
     * with the given label. Return a reference to the new state (convenience)
     *
     * NOTE: all transitions must be added before the State is actually used.
     */
    public State addTransition (Character a, State s)
    {
        __transitions.put (a, s);
        return s;
    }


    public Set<String> getOutput ()
    {
        return __output;
    }


    public void addOutput (String output)
    {
        if (output != null)
        {
            __output.add (output);
        }
        else
        {
            log.warning ("Adding null output string");
        }
    }


    public void mergeOutput (Set<String> output)
    {
        __output.addAll (output);
    }

    
    public void setFailure (State s)
    {
        __failure = s;
    }


    public State getFailure ()
    {
        return __failure;
    }


    public boolean isStart ()
    {
        return __id == 0;
    }


    /**
     * Return state that we would transition to on input a. Returns
     * null if there is no transition for the input character
     * i.e. does not follow failure paths.
     */
    public State goTo (Character a)
    {
        State target = __transitions.get (a);
        if (target == null && this.isStart()) 
        {
            // Start state loops
            return this;
        }
        else
        {
            return target;
        }
    }


    /**
     * Iterable interface method
     */
    public Iterator<State> iterator ()
    {
        return new StateIterator (this);
    }


    public String toString ()
    {
        StringBuffer sb = new StringBuffer ();
        sb.append (String.format("[id=%s][failure=%s][transitions[", 
                                 __id, __failure == null ? "" : __failure.getId()));
        for ( Character c : __transitions.keySet() )
        {
            sb.append ("[" + c.toString() + " -> " + __transitions.get(c).getId() + "]");
        }
        sb.append ("]");
        return sb.toString ();
    }
}


