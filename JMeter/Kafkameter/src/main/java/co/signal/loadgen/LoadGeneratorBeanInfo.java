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

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.reflect.ClassFinder;
import org.apache.log.Logger;

/**
 * Creates the fields for the {@link LoadGenerator} GUI in JMeter.
 *
 * @author codyaray
 * @since 6/30/14
 */
public class LoadGeneratorBeanInfo extends BeanInfoSupport {

  private static final Logger log = LoggingManager.getLoggerForClass();

  private static final String VARIABLE_NAME = "variableName";
  private static final String CLASS_NAME = "className";
  private static final String VARIABLE_TAG = "variableTag";
  private static final String VARIABLE_ID = "variableSensorId";

  public LoadGeneratorBeanInfo() {
    super(LoadGenerator.class);

    createPropertyGroup("load_generator", new String[] {
        CLASS_NAME, VARIABLE_NAME, VARIABLE_TAG, VARIABLE_ID
    });

    List<String> classes = findAvailableImplementations();
    PropertyDescriptor p = property(CLASS_NAME);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, classes.get(0));
    p.setValue(NOT_EXPRESSION, Boolean.TRUE);
    p.setValue(NOT_OTHER, Boolean.TRUE);
    p.setValue(TAGS, Iterables.toArray(classes, String.class));

    p = property(VARIABLE_NAME);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, "");
    p.setValue(NOT_EXPRESSION, Boolean.TRUE);

    p = property(VARIABLE_TAG);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, "");
    p.setValue(NOT_EXPRESSION, Boolean.TRUE);

    p = property(VARIABLE_ID);
    p.setValue(NOT_UNDEFINED, Boolean.TRUE);
    p.setValue(DEFAULT, 0);
    p.setValue(NOT_EXPRESSION, Boolean.FALSE);

  }

  private static List<String> findAvailableImplementations() {
    try {
      return ClassFinder.findClassesThatExtend(
          JMeterUtils.getSearchPaths(), new Class[] { SyntheticLoadGenerator.class });
    } catch (IOException e) {
      log.fatalError("Exception finding SyntheticLoadGenerator implementations", e);
      throw Throwables.propagate(e);
    }
  }
}
