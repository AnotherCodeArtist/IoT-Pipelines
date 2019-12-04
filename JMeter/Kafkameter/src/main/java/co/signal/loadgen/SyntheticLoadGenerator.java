/*
 * Copyright 2014 Signal.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.signal.loadgen;

/**
 * Generates synthetic load specific to an application's input message format and load distribution.
 *
 * Implementations must have a constructor which takes the configuration as a single
 * {@link javax.annotation.Nullable @Nullable} {@link String} argument. This string optionally
 * contains a Synthetic Load Description which will have a format specific to each application.
 *
 * @author codyaray
 * @since 7/17/14
 */
public interface SyntheticLoadGenerator {

  /**
   * Returns the next generated message.
   * This method is called on each JMeter iteration.
   *
   * @return the next generated message.
   */
  String nextMessage();
  String nextTag();
}
