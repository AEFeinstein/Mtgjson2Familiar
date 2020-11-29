#!/bin/bash

# Set up some variables
DATE=$(date +"%Y-%m-%d")
BRANCH_NAME=AUTO_$DATE

# Reset the repo to main
git checkout main
git remote prune origin
git reset --hard
git pull

# Create and checkout a new branch
git checkout -b $BRANCH_NAME

# TODO run the updates
touch newfile.txt

# Commit and push the changes
git add --all
git commit -m "Automatic Updates"
git push --set-upstream origin $BRANCH_NAME

# Create the pull request
gh auth login --with-token < gh-token.txt
gh pr create --title "Automatic Updates $DATE" --body "See the diff"