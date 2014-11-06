# TW Scripts

This is a collection of scripts I use to interact with Twitter. It is not meant to be a library, so if you wish to use the scripts, just clone the repository and follow along. If you think something in this project should be a library, please let me know and I will consider it.

## Configuration

In order to use these scripts, you will need to create a presonal [Twitter application](https://apps.twitter.com/). Once you have all the information for your Twitter application handy, you must create a `~/.tw.edn` file that looks like the one below but with your values filled in.

```
{:consumer-key "YOUR_CONSUMER_KEY"
 :consumer-secret "YOUR_CONSUMER_SECRET"
 :access-token "YOUR_ACCESS_TOKEN"
 :access-token-secret "YOUR_ACCESS_TOKEN_SECRET"
 :access-level :read-only ;; :read-write, :read-write-dm
 :owner "YOUR_TWITTER_HANDLE"
 :owner-id "YOUR_TWITTER_ID"}
```

## Followers

This script will download your followers list to a file.

```
$ lein run -m tw-scripts.followers
Downloaded 282 followers.
```

If you wish, you can load the `tw-scripts.followers` namespace into a REPL.

```
user> (require 'tw-scripts.followers)
nil
user> (tw-scripts.followers/get-followers)
({"screen_name" "jeremyheiler" "id_str" "20820543"})
user> (count *1)
1
```

The `get-followers` function is a chunked lazy sequence, with a default chunk size of 20. This means that it your followers list will be requested from Twitter as you need them. You can override the chunk size with the `:followers-per-request` option in your configuration. Twitter will not allow you to use a chunk size larger than 200, and this script enforces that.

### Followers Configuration

There are two optional configuration options for the Followers script.

* `:data-dir`: This is the full path to the directory you wish to save the file that will contain your followers list. If not specified, it will default to your home directory. This option is only used by `tw-scripts.followers/save` and the `-main` function.

* `:followers-per-request` This option specifies how many followers to get per request. The default value is 20. The maximum value allowed is 200, as per the Twitter API documentation.

## License

Copyright © 2014 Jeremy Heiler

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
