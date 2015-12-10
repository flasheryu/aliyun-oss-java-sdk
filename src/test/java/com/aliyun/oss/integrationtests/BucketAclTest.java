/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ALREADY_EXIST_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ACCESS_DENIED_ERR;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AccessControlList;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.Grant;
import com.aliyun.oss.model.GroupGrantee;
import com.aliyun.oss.model.Permission;

public class BucketAclTest extends TestBase {

    private static final CannedAccessControlList[] acls = {
        null, 
        CannedAccessControlList.Private, 
        CannedAccessControlList.PublicRead, 
        CannedAccessControlList.PublicReadWrite 
    };
    
    @Test
    public void testNormalSetBucketAcl() {
        final String bucketName = "normal-set-bucket-acl";
        
        try {
            secondClient.createBucket(bucketName);
            
            for (CannedAccessControlList acl : acls) {
                secondClient.setBucketAcl(bucketName, acl);
                
                AccessControlList returnedAcl = secondClient.getBucketAcl(bucketName);
                if (acl != null && !acl.equals(CannedAccessControlList.Private)) {
                    Set<Grant> grants = returnedAcl.getGrants();
                    Assert.assertEquals(1, grants.size());
                    Grant grant = (Grant) grants.toArray()[0];
                    
                    if (acl.equals(CannedAccessControlList.PublicRead)) {
                        Assert.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
                        Assert.assertEquals(Permission.Read, grant.getPermission());
                    } else if (acl.equals(CannedAccessControlList.PublicReadWrite)) {                        
                        Assert.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
                        Assert.assertEquals(Permission.FullControl, grant.getPermission());
                    }
                }
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            secondClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalSetBucketAcl() {
        final String bucketName = "unormal-set-bucket-acl";
        
        try {
            secondClient.createBucket(bucketName);
            
            // Set non-existent bucket
            final String nonexistentBucket = "unormal-set-bucket-acl";
            try {
                secondClient.setBucketAcl(nonexistentBucket, CannedAccessControlList.Private);
                // TODO: Why not failed with NO_SUCK_BUCKET error code ?
                //Assert.fail("Set bucket acl should not be successful");
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            
            // Set bucket without ownership
            final String bucketWithoutOwnership = "oss";
            try {
                secondClient.setBucketAcl(bucketWithoutOwnership, CannedAccessControlList.Private);
                Assert.fail("Set bucket acl should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.BUCKET_ALREADY_EXISTS, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(BUCKET_ALREADY_EXIST_ERR));
            }
            
            // Set illegal acl
            final String illegalAcl = "IllegalAcl";
            try {
                CannedAccessControlList.parse(illegalAcl);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            secondClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalGetBucketAcl() {
        // Get non-existent bucket
        final String nonexistentBucket = "unormal-get-bucket-acl";
        try {
            secondClient.getBucketAcl(nonexistentBucket);
            Assert.fail("Get bucket acl should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
        
        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            secondClient.getBucketAcl(bucketWithoutOwnership);
            Assert.fail("Get bucket referer should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }
        
        // Get bucket using default acl
        final String bucketUsingDefaultAcl = "bucket-using-default-acl";
        try {
            secondClient.createBucket(bucketUsingDefaultAcl);
            
            AccessControlList returnedACL = secondClient.getBucketAcl(bucketUsingDefaultAcl);
            Set<Grant> grants = returnedACL.getGrants();
            // No grants when using default acl
            Assert.assertEquals(0, grants.size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            secondClient.deleteBucket(bucketUsingDefaultAcl);
        }
    }
}
