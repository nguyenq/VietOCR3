/**
 * Copyright @ 2008 Quan Nguyen
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

package net.sourceforge.vietocr.postprocessing;

public class ProcessorFactory {
    
     public static IPostProcessor createProcessor(ISO639 code) {
        IPostProcessor processor;
        
        switch (code) {
            case eng:
                processor = new EngPP();
                break;
            case vie:
                processor = new ViePP();
                break;
            default:
                processor = new EngPP();
                break;
//                throw new UnsupportedOperationException(code.toString());
        }
        
        return processor;
    }
}
