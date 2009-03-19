/**
 * Copyright (c) 2009 Espen Wiborg <espenhw@grumblesmurf.org>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */ 
package org.grumblesmurf.malabar;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.hamcrest.CoreMatchers.*;

class TypestringTest 
{
    def cp;

    @Before
    void createClasspath() {
        cp = new Classpath();
    }
    
    @Test
    void typeStringOfObject() {
        assertThat(cp.typeString(Object.class), is("Object"))
    }
    
    @Test
    void qualifiedTypeStringOfObject() {
        assertThat(cp.typeString(Object.class, true), is("java.lang.Object"))
    }
    
    @Test
    void typeStringOfCollections() {
        assertThat(cp.typeString(Collections.class), is("Collections"))
    }
    
    @Test
    void qualifiedTypeStringOfCollections() {
        assertThat(cp.typeString(Collections.class, true), is("java.util.Collections"))
    }

    @Test
    void collectionIsGeneric() {
        assertThat(cp.typeString(Collection.class, true), is("java.util.Collection<E>"))
    }

    @Test
    void mapHasTwoTypeparams() {
        assertThat(cp.typeString(Map.class, true), is("java.util.Map<K, V>"))
    }

    @Test
    void collectionsAddAllHasGenericFirstParam() {
        assertThat(cp.typeString(Collections.methods.find {it.name == 'addAll'}.genericParameterTypes[0], true),
                   is("java.util.Collection<? super T>"))
    }

    @Test
    void collectionsBinarySearchHasComplicatedFirstParam() {
        assertThat(cp.typeString(Collections.methods.find {it.name == 'binarySearch'}.genericParameterTypes[0], true),
                   is("java.util.List<? extends java.lang.Comparable<? super T>>"))
    }

    @Test
    void collectionsMaxHasAbsurdlyComplicatedTypeParam() {
        assertThat(cp.typeString(Collections.getMethod("max", [ Collection ] as Class[]).typeParameters[0], true),
                   is("T extends java.lang.Object & java.lang.Comparable<? super T>"))
    }

    @Test
    void collectionsMaxHasAbsurdlyComplicatedTypeParamUnqualified() {
        assertThat(cp.typeString(Collections.getMethod("max", [ Collection ] as Class[]).typeParameters[0]),
                   is("T extends Object & Comparable<? super T>"))
    }

    @Test
    void enumHasRecursiveInterface() {
        assertThat(cp.typeString(Enum.genericInterfaces[0], true),
                   is("java.lang.Comparable<E>"));
    }
}