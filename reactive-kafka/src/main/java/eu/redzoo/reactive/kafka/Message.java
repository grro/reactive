package eu.redzoo.reactive.kafka;

import com.google.common.base.MoreObjects;






public interface Message {
    
    String getId();
   
    String getData();
    
    
    public static Message newMessage(long id, String data) {
        return newMessage(Long.toString(id), data);
    }
    
    public static Message newMessage(String id, String data) {
        
        return new Message() {
    
            @Override
            public String getId() {
                return id;
            }
    
            @Override
            public String getData() {
                return data;
            }
            
            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this)
                                  .add("id", id)
                                  .add("data", data)
                                  .toString();
            }
        };
    }
}