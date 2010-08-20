/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.junit.Test;

import com.mysema.query.QueryFlag.Position;
import com.mysema.query.types.path.PString;
import com.mysema.util.ReflectionUtils;

public class DefaultQueryMetadataTest {

    private QueryMetadata metadata = new DefaultQueryMetadata();

    private PString str = new PString("str");

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException{
        PString expr = new PString("str");
        metadata.addFlag(new QueryFlag(Position.AFTER_FILTERS, ""));
        metadata.addGroupBy(expr);
        metadata.addHaving(expr.isEmpty());
        metadata.addJoin(JoinType.DEFAULT, expr);
        metadata.getJoins().get(0).addFlag(new JoinFlag(""));
        metadata.addJoinCondition(expr.isEmpty());
        metadata.addOrderBy(expr.asc());
        metadata.addProjection(expr);
        metadata.addWhere(expr.isEmpty());
        
        // serialize metadata
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(metadata);
        out.close();
        
        // deserialize metadata
        ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bain);
        QueryMetadata  metadata2 = (QueryMetadata) in.readObject();
        in.close();
        
        assertEquals(metadata.getFlags(),     metadata2.getFlags());
        assertEquals(metadata.getGroupBy(),   metadata2.getGroupBy());
        assertEquals(metadata.getHaving(),    metadata2.getHaving());
        assertEquals(metadata.getJoins(),     metadata2.getJoins());
        assertEquals(metadata.getModifiers(), metadata2.getModifiers());
        assertEquals(metadata.getOrderBy(),   metadata2.getOrderBy());
        assertEquals(metadata.getParams(),    metadata2.getParams());
        assertEquals(metadata.getProjection(),metadata2.getProjection());
        assertEquals(metadata.getWhere(),     metadata2.getWhere());
    }
    
    @Test
    public void testFullySerizable(){
        Set<Class<?>> checked = new HashSet<Class<?>>();
        checked.addAll(Arrays.<Class<?>>asList(List.class, Set.class, Map.class, Class.class, String.class, Object.class));
        Stack<Class<?>> classes = new Stack<Class<?>>();
        classes.add(DefaultQueryMetadata.class);
        while (!classes.isEmpty()){            
            Class<?> clazz = classes.pop();
            checked.add(clazz);
            if (!Serializable.class.isAssignableFrom(clazz) && !clazz.isPrimitive()){                
                fail(clazz.getName() + " is not serializable");
            }            
            for (Field field : clazz.getDeclaredFields()){
                Set<Class<?>> types = new HashSet<Class<?>>(3);
                types.add(field.getType());
                if (field.getType().getComponentType() != null){
                    types.add(field.getType().getComponentType());
                }
                if (Collection.class.isAssignableFrom(field.getType())){
                    types.add(ReflectionUtils.getTypeParameter(field.getGenericType(), 0)); 
                }else if (Map.class.isAssignableFrom(field.getType())){
                    types.add(ReflectionUtils.getTypeParameter(field.getGenericType(), 0));
                    types.add(ReflectionUtils.getTypeParameter(field.getGenericType(), 1));
                }
                types.removeAll(checked);
                classes.addAll(types);
            }
        }
    }
    
    @Test
    public void testGetGroupBy() {
        metadata.addGroupBy(str);
        assertEquals(Arrays.asList(str), metadata.getGroupBy());
    }

    @Test
    public void testGetHaving() {
        metadata.addHaving(str.isNotNull());
        assertEquals(str.isNotNull(), metadata.getHaving());
    }

    @Test
    public void testGetJoins() {
        metadata.addJoin(JoinType.DEFAULT, str);
        assertEquals(Arrays.asList(new JoinExpression(JoinType.DEFAULT, str)),metadata.getJoins());
    }

    @Test
    public void testGetModifiers() {
        QueryModifiers modifiers = new QueryModifiers(1l,2l);
        metadata.setModifiers(modifiers);
        assertEquals(modifiers, metadata.getModifiers());
    }

    @Test
    public void setLimit(){
        QueryModifiers modifiers = new QueryModifiers(1l,2l);
        metadata.setModifiers(modifiers);
        metadata.setLimit(3l);

        assertEquals(Long.valueOf(3l), metadata.getModifiers().getLimit());
        assertEquals(Long.valueOf(2l), metadata.getModifiers().getOffset());
    }

    @Test
    public void setOffset(){
        QueryModifiers modifiers = new QueryModifiers(1l,1l);
        metadata.setModifiers(modifiers);
        metadata.setOffset(2l);

        assertEquals(Long.valueOf(1l), metadata.getModifiers().getLimit());
        assertEquals(Long.valueOf(2l), metadata.getModifiers().getOffset());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetOrderBy() {
        metadata.addOrderBy(str.asc());
        metadata.addOrderBy(str.desc());
        assertEquals(Arrays.asList(str.asc(),str.desc()), metadata.getOrderBy());
    }

    @Test
    public void testGetProjection() {
        metadata.addProjection(str, str.append("abc"));
        assertEquals(Arrays.asList(str, str.append("abc")), metadata.getProjection());
    }

    @Test
    public void testGetWhere() {
        metadata.addWhere(str.eq("b"), str.isNotEmpty());
        assertEquals(str.eq("b").and(str.isNotEmpty()), metadata.getWhere());
    }

    @Test
    public void testIsDistinct() {
        assertFalse(metadata.isDistinct());
        metadata.setDistinct(true);
        assertTrue(metadata.isDistinct());
    }

    @Test
    public void testIsUnique() {
        assertFalse(metadata.isUnique());
        metadata.setUnique(true);
        assertTrue(metadata.isUnique());
    }

    @Test
    public void testClone(){
        metadata.addGroupBy(str);
        metadata.addHaving(str.isNotNull());
        metadata.addJoin(JoinType.DEFAULT, str);
        QueryModifiers modifiers = new QueryModifiers(1l,2l);
        metadata.setModifiers(modifiers);
        metadata.addOrderBy(str.asc());
        metadata.addProjection(str, str.append("abc"));
        metadata.addWhere(str.eq("b"), str.isNotEmpty());

        QueryMetadata clone = metadata.clone();
        assertEquals(metadata.getGroupBy(), clone.getGroupBy());
        assertEquals(metadata.getHaving(), clone.getHaving());
        assertEquals(metadata.getJoins(), clone.getJoins());
        assertEquals(metadata.getModifiers(), clone.getModifiers());
        assertEquals(metadata.getOrderBy(), clone.getOrderBy());
        assertEquals(metadata.getProjection(), clone.getProjection());
        assertEquals(metadata.getWhere(), clone.getWhere());

    }
}
