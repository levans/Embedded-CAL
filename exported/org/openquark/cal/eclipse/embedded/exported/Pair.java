/*
 * Copyright (c) 2007 BUSINESS OBJECTS SOFTWARE LIMITED
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *  
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *  
 *     * Neither the name of Business Objects nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


/*
 * Pair.java
 * Created: Jun 18, 2007
 * By: Andrew Eisenberg
 */
package org.openquark.cal.eclipse.embedded.exported;


/**
 * A class that holds a pair of generic objects.
 * 
 * @author Edward Lam  (original author)
 * @author Andrew Eisenberg  (added generics)
 * @param <T> 
 * @param <U> 
 */
public class Pair<T, U> {

    /** The first object of the pair. */
    private final T object1;

    /** The second object of the pair. */
    private final U object2;

    /**
     * Constructor for a Pair object.
     * @param object1 Object the first object of the pair.
     * @param object2 Object the second object of the pair.
     */
    public Pair(T object1, U object2) {
        this.object1 = object1;
        this.object2 = object2;
    }

    /**
     * Get the first object of the pair.
     * @return Object the first object of the pair.
     */
    public T fst() {
        return object1;
    }

    /**
     * Get the second object of the pair.
     * @return Object the second object of the pair.
     */
    public U snd() {
        return object2;
    }

    /**
     * Return whether this object is "equal" to another.
     * This is true if the corresponding elements of the pairs are equals().
     * @param obj Object the other object.
     * @return boolean true if "equal"
     */
    public boolean equals(Object obj) {

        if (!(obj instanceof Pair)) {
            return false;
        }

        // true if the corresponding objects in each pair match.
        Pair<?,?> otherPair = (Pair<?,?>)obj;
        return (object1 == null ? otherPair.object1 == null : object1.equals(otherPair.object1)) && 
        (object2 == null ? otherPair.object2 == null : object2.equals(otherPair.object2));
    }

    /**
     * Return a hashcode for this object.
     * @return int the pair's hash code
     */
    public int hashCode() {
        // make sure that two objects that are equals() have the same hashCode (for use in collections)
        int object1HashCode = object1 == null ? 0 : object1.hashCode();
        int object2HashCode = object2 == null ? 0 : object2.hashCode();
        return 37 * (17 + object1HashCode) + object2HashCode;
    }

    /**
     * A reasonable string representation for this class.
     * @return the string representation for this class.
     */
    public String toString() {
        return "Pair: (" + object1 + ", " + object2 + ")";
    }

}
