/*
 * Copyright (c) 2014 Gregor Roth
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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