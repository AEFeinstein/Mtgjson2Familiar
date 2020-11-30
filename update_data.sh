#!/bin/bash

# Set up some variables
DATE=$(date +"%Y-%m-%d")
BRANCH_NAME=AUTO_$DATE

# Reset the repo to main
git checkout main
git branch | grep -v "main" | xargs git branch -D 
git reset --hard
git pull

# Create and checkout a new branch
git checkout -b $BRANCH_NAME

# Run the updates
java -jar Mtgjson2Familiar.jar $@

# Commit and push the changes
git add --all
git commit -m "Automatic Updates"
git push --set-upstream origin $BRANCH_NAME

# Create the pull request
gh auth login --with-token < gh-token.txt
gh pr create --title "Automatic Updates $DATE" --body "See the diff"
