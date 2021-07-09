# How to release a new version of ktoml

* You should have permissions to push to the main repo
* Simply create a new git tag with format `v*` and push it. Github workflow will perform release automatically.
  
  For example:
  ```bash
  $ git tag v1.0.0
  $ git push origin --tags 
  ```
  
After the release workflow has started, version number is determined from tag. Binaries are uploaded to maven repo and 
a new github release is created with fat jar.

* We are using both maven central and github to publish releases. 
To prevent invalid files reaching Nexus you should manually promote releases in [staging](https://s01.oss.sonatype.org/#stagingRepositories)
