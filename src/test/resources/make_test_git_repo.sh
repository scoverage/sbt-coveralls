#!/usr/bin/env bash

echo "Creating test git repo in /tmp/xsbt-coveralls-plugin/test_repo"

rm -fR /tmp/xsbt-coveralls-plugin/test_repo
mkdir -p /tmp/xsbt-coveralls-plugin/test_repo
cd /tmp/xsbt-coveralls-plugin/test_repo

echo "This is a test git repo for GitClientTest" > README.md

cd /tmp/xsbt-coveralls-plugin/test_repo

git init
git config user.email "test_user@test_email.com"
git config user.name "test_username"
git add README.md
git commit -am"Commit message for unit test"
git remote add origin_test_1 git@origin_test_1
git remote add origin_test_2 git@origin_test_2

echo "Created test git repo in /tmp/xsbt-coveralls-plugin/test_repo"