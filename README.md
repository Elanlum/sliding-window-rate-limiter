Demo-project contains quick look at rate-limiting using Redis.

There are two options for rate limiting described in current project:

1. Fixed window
2. Sliding window

You can find function for each case in main directory.

Use docker image for Redis, for instance:

<code>
docker run -p 16379:6379 -d redis:6.0 redis-server --requirepass "mypass"
</code>

or setup Redis locally using brew:

<code>
brew install redis
</code>

Using redis-cli you will be able to access records of request pushed into Redis

<code>
zrange rate_limit_localhost 0 -1 WITHSCORES
</code>

If you use password like above for Redis in docker, before access contents you need to authorize:

Enter to redis-cli:
<code>
auth %mypass%
</code>

