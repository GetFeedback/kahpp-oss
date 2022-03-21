# Kahpp release process

We keep our release process as straightforward as possible.  
We use a bot called EVE to automate a big part of this process.  
Thanks to EVE, we don't need to manage milestones and releases.  

The release process follows these steps:

* Merge the changes on the main branch
* Create a [tag](#tag) with a [version](#versioning) as the title and the [changelog](#changelog) as the message.
* Push the tag, a publish build will start
* Now EVE will do the rest (Close milestone, open new milestone, create release)

## Versioning

The Kahpp releases adhere to [semantic versioning](https://semver.org/).

So in summary:

Given a version number MAJOR.MINOR.PATCH, increment the:

* MAJOR version when you make incompatible API changes,
* MINOR version when you add functionality in a backwards compatible manner, and
* PATCH version when you make backwards compatible bug fixes.

## Milestones

We use milestones to track the progress on groups.  
Milestones are managed by EVE.  
EVE is a bot that help us to automate the release process.

## Changelog

To generate the changelog we use a tool that generate it based on GitHub milestones, [jwage/changelog-generator](https://github.com/jwage/changelog-generator).

## Tag

For the commit that you want to create into a release, create a signed tag that matches the milestone name and contains the changelog.
```
git tag -s x.y.z
```
Once you are ready push the tag
```
git push origin x.y.z
```

## Publish artifacts

The release build runs automatically on a new tag.  
After a couple o minutes a new release appears on the [GitHub release page](https://github.com/GetFeedback/kahpp-oss/releases).  
