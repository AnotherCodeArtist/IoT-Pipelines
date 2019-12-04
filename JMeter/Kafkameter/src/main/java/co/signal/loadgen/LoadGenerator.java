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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 * Config Element which reads a Synthetic Load Description from a file, generates
 * a domain-specific message, and exports the message under a given variableName.
 *
 * @author codyaray
 * @since 6/27/14
 */
public class LoadGenerator extends ConfigTestElement implements TestBean, LoopIterationListener {

  private static final Logger log = LoggingManager.getLoggerForClass();

  private int variableSensorId;
  private String variableName;
  private String className;
  private String variableTag;

  private SyntheticLoadGenerator generator;

  @Override
  public void iterationStart(LoopIterationEvent loopIterationEvent) {
    if (generator == null) {
      generator = createGenerator(getClassName(), getVariableSensorId());
    }
    JMeterVariables variables = JMeterContextService.getContext().getVariables();
    variables.put(getVariableName(), generator.nextMessage());
    variables.put(getVariableTag(),  generator.nextTag());
  }

  private SyntheticLoadGenerator createGenerator(String className, @Nullable int config) {
    try {
      return (SyntheticLoadGenerator) Class.forName(
          className, false, Thread.currentThread().getContextClassLoader()
      ).getConstructor(Integer.class).newInstance(config);
    } catch (Exception e) {
      log.fatalError("Exception initializing Load Generator class: " + className, e);
      throw Throwables.propagate(e);
    }
  }

  /**
   * @return the variableName
   */
  public String getVariableName() {
    return variableName;
  }

  /**
   * @param variableName the variableName to set
   */
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }
    /**
   * @return the variableName
   */
  public String getVariableTag() {
    return variableTag;
  }

  /**
   * @param variableName the variableName to set
   */
  public void setVariableTag(String variableTag) {
    this.variableTag = variableTag;
  }


  /**
   * @return the fileName
   */
  public int getVariableSensorId() {
    return variableSensorId;
  }

  /**
   * @param fileName the fileName to set
   */
  public void setVariableSensorId(int variableSensorId) {
    this.variableSensorId = variableSensorId;
  }

  /**
   * @return the className
   */
  public String getClassName() {
    return className;
  }

  /**
   * @param className the className used for generating the messages
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * Helper for testing outside of JMeter
   */
  public static void main(String[] args) {
    LoadGenerator generator = new LoadGenerator();

    // Mock out JMeter environment
    JMeterVariables variables = new JMeterVariables();
    JMeterContextService.getContext().setVariables(variables);
    //generator.setFileName("config1.json");
    generator.setVariableName("kafka_message");

    generator.iterationStart(null);

    for (Map.Entry<String, Object> entry : variables.entrySet()) {
      System.out.println(entry.getKey() + " : " + entry.getValue());
    }
  }
}
