package org.springframework.integration.strictordering.entitykey;
/**
 * 
 * @author David Turanski
 *
 * @param <E>
 * @param <K>
 */
public interface EntityKeyExtractor<E,K> {
   public K getKey(E entity);
}