/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and
 *     "Apache Jetspeed" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache" or
 *    "Apache Jetspeed", nor may "Apache" appear in their name, without
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.jetspeed.util;

/**
 * This class provides static util methods for String manaipulation that
 * aren't part of the default JDK functionalities.
 *
 * @author <a href="mailto:raphael@apache.org">Raphaël Luta</a>
 * @version $Id$
 */
public class StringUtils
{
    /**
     * Replaces all the occurences of a substring found
     * within a StringBuffer by a
     * replacement string
     *
     * @param buffer the StringBuffer in where the replace will take place
     * @param find the substring to find and replace
     * @param replacement the replacement string for all occurences of find
     * @returns the original StringBuffer where all the
     * occurences of find are replaced by replacement
     */
    public static StringBuffer replaceAll(StringBuffer buffer, String find, String replacement)
    {
        int bufidx = buffer.length() - 1;
        int offset = find.length();
        while( bufidx > -1 ) {
            int findidx = offset -1;
            while( findidx > -1 ) {
                if( bufidx == -1 ) {
                    //Done
                    return buffer;
                }
                if( buffer.charAt( bufidx ) == find.charAt( findidx ) ) {
                    findidx--; //Look for next char
                    bufidx--;
                } else {
                    findidx = offset - 1; //Start looking again
                    bufidx--;
                    if( bufidx == -1 ) {
                        //Done
                        return buffer;
                    }
                    continue;
                }
            }
            //Found
            //System.out.println( "replacing from " + (bufidx + 1) +
            //                    " to " + (bufidx + 1 + offset ) +
            //                    " with '" + replacement + "'" );
            buffer.replace( bufidx+1,
                            bufidx+1+offset,
                            replacement);
            //start looking again
        }
        //No more matches
        return buffer;
    }
}