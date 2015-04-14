#!/bin/bash

export R_HOME=/usr/lib/R

cp RCode/init.r .Rprofile


# R --vanilla --interactive -f RCode/init.r
R --no-restore --no-site-file --no-environ --no-save --interactive


