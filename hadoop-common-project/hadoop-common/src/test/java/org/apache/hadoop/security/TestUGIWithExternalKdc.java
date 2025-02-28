/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.hadoop.security;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import static org.apache.hadoop.security.SecurityUtilTestHelper.isExternalKdcRunning;
import org.apache.hadoop.security.UserGroupInformation.AuthenticationMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests kerberos keytab login using a user-specified external KDC
 *
 * To run, users must specify the following system properties:
 *   externalKdc=true
 *   java.security.krb5.conf
 *   user.principal
 *   user.keytab
 */
public class TestUGIWithExternalKdc {

  @BeforeEach
  public void testExternalKdcRunning() {
    assumeTrue(isExternalKdcRunning());
  }

  @Test
  public void testLogin() throws IOException {
    String userPrincipal = System.getProperty("user.principal");
    String userKeyTab = System.getProperty("user.keytab");
    assertNotNull(userPrincipal, "User principal was not specified");
    assertNotNull(userKeyTab, "User keytab was not specified");

    Configuration conf = new Configuration();
    conf.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTHENTICATION,
        "kerberos");
    UserGroupInformation.setConfiguration(conf);

    UserGroupInformation ugi = UserGroupInformation
        .loginUserFromKeytabAndReturnUGI(userPrincipal, userKeyTab);

    assertEquals(AuthenticationMethod.KERBEROS,
        ugi.getAuthenticationMethod());
    
    try {
      UserGroupInformation
      .loginUserFromKeytabAndReturnUGI("bogus@EXAMPLE.COM", userKeyTab);
      fail("Login should have failed");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

}
