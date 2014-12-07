/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.comci.bitmap;

/**
 *
 * @author Sebastian Maier (sebastian.maier@comci.de)
 */
class DefaultCollectionBuilder implements CollectionBuilder {
    
    private final BitMapCollection collection;

    public DefaultCollectionBuilder(BitMapCollection collection) {
        this.collection = collection;
    }

    @Override
    public CollectionBuilder add(Object... data) {
        this.collection.add(data);
        return this;
    }

    @Override
    public BitMapCollection build() {
        collection.build();
        return collection;
    }
    
}
