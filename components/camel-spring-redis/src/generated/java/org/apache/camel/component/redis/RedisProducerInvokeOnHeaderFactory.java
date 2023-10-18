/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.redis;

import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.spi.InvokeOnHeaderStrategy;
import org.apache.camel.component.redis.RedisProducer;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class RedisProducerInvokeOnHeaderFactory implements InvokeOnHeaderStrategy {

    @Override
    public Object invoke(Object obj, String key, Exchange exchange, AsyncCallback callback) throws Exception {
        org.apache.camel.component.redis.RedisProducer target = (org.apache.camel.component.redis.RedisProducer) obj;
        switch (key) {
        case "append":
        case "APPEND": return target.invokeAppend(exchange);
        case "blpop":
        case "BLPOP": return target.invokeBlpop(exchange);
        case "brpop":
        case "BRPOP": return target.invokeBrpop(exchange);
        case "brpoplpush":
        case "BRPOPLPUSH": return target.invokeBrpoplpush(exchange);
        case "decr":
        case "DECR": return target.invokeDecr(exchange);
        case "decrby":
        case "DECRBY": return target.invokeDecrby(exchange);
        case "del":
        case "DEL": target.invokeDel(exchange); return null;
        case "discard":
        case "DISCARD": target.invokeDiscard(exchange); return null;
        case "echo":
        case "ECHO": return target.invokeEcho(exchange);
        case "exec":
        case "EXEC": target.invokeExec(exchange); return null;
        case "exists":
        case "EXISTS": return target.invokeExists(exchange);
        case "expire":
        case "EXPIRE": return target.invokeExpire(exchange);
        case "expireat":
        case "EXPIREAT": return target.invokeExpireat(exchange);
        case "geoadd":
        case "GEOADD": return target.invokeGeoadd(exchange);
        case "geodist":
        case "GEODIST": return target.invokeGeodist(exchange);
        case "geohash":
        case "GEOHASH": return target.invokeGeohash(exchange);
        case "geopos":
        case "GEOPOS": return target.invokeGeopos(exchange);
        case "georadius":
        case "GEORADIUS": return target.invokeGeoradius(exchange);
        case "georadiusbymember":
        case "GEORADIUSBYMEMBER": return target.invokeGeoradiusbymember(exchange);
        case "get":
        case "GET": return target.invokeGet(exchange);
        case "getbit":
        case "GETBIT": return target.invokeGetbit(exchange);
        case "getrange":
        case "GETRANGE": return target.invokeGetrange(exchange);
        case "getset":
        case "GETSET": return target.invokeGetset(exchange);
        case "hdel":
        case "HDEL": target.invokeHdel(exchange); return null;
        case "hexists":
        case "HEXISTS": return target.invokeHexists(exchange);
        case "hget":
        case "HGET": return target.invokeHget(exchange);
        case "hgetall":
        case "HGETALL": return target.invokeHgetAll(exchange);
        case "hincrby":
        case "HINCRBY": return target.invokeHincrBy(exchange);
        case "hkeys":
        case "HKEYS": return target.invokeHkeys(exchange);
        case "hlen":
        case "HLEN": return target.invokeHlen(exchange);
        case "hmget":
        case "HMGET": return target.invokeHmget(exchange);
        case "hmset":
        case "HMSET": target.invokeHmset(exchange); return null;
        case "hset":
        case "HSET": target.invokeHset(exchange); return null;
        case "hsetnx":
        case "HSETNX": return target.invokeHsetnx(exchange);
        case "hvals":
        case "HVALS": return target.invokeHvals(exchange);
        case "incr":
        case "INCR": return target.invokeIncr(exchange);
        case "incrby":
        case "INCRBY": return target.invokeIncrby(exchange);
        case "keys":
        case "KEYS": return target.invokeKeys(exchange);
        case "lindex":
        case "LINDEX": return target.invokeLindex(exchange);
        case "linsert":
        case "LINSERT": return target.invokeLinsert(exchange);
        case "llen":
        case "LLEN": return target.invokeLlen(exchange);
        case "lpop":
        case "LPOP": return target.invokeLpop(exchange);
        case "lpush":
        case "LPUSH": return target.invokeLpush(exchange);
        case "lpushx":
        case "LPUSHX": return target.invokeLpushx(exchange);
        case "lrange":
        case "LRANGE": return target.invokeLrange(exchange);
        case "lrem":
        case "LREM": return target.invokeLrem(exchange);
        case "lset":
        case "LSET": target.invokeLset(exchange); return null;
        case "ltrim":
        case "LTRIM": target.invokeLtrim(exchange); return null;
        case "mget":
        case "MGET": return target.invokeMget(exchange);
        case "move":
        case "MOVE": return target.invokeMove(exchange);
        case "mset":
        case "MSET": target.invokeMset(exchange); return null;
        case "msetnx":
        case "MSETNX": target.invokeMsetnx(exchange); return null;
        case "multi":
        case "MULTI": target.invokeMulti(exchange); return null;
        case "persist":
        case "PERSIST": return target.invokePersist(exchange);
        case "pexpire":
        case "PEXPIRE": return target.invokePexpire(exchange);
        case "pexpireat":
        case "PEXPIREAT": return target.invokePexpireat(exchange);
        case "ping":
        case "PING": return target.invokePing(exchange);
        case "publish":
        case "PUBLISH": target.invokePublish(exchange); return null;
        case "quit":
        case "QUIT": target.invokeQuit(exchange); return null;
        case "randomkey":
        case "RANDOMKEY": return target.invokeRandomkey(exchange);
        case "rename":
        case "RENAME": target.invokeRename(exchange); return null;
        case "renamenx":
        case "RENAMENX": return target.invokeRenamenx(exchange);
        case "rpop":
        case "RPOP": return target.invokeRpop(exchange);
        case "rpoplpush":
        case "RPOPLPUSH": return target.invokeRpoplpush(exchange);
        case "rpush":
        case "RPUSH": return target.invokeRpush(exchange);
        case "rpushx":
        case "RPUSHX": return target.invokeRpushx(exchange);
        case "sadd":
        case "SADD": return target.invokeSadd(exchange);
        case "scard":
        case "SCARD": return target.invokeScard(exchange);
        case "sdiff":
        case "SDIFF": return target.invokeSdiff(exchange);
        case "sdiffstore":
        case "SDIFFSTORE": target.invokeSdiffstore(exchange); return null;
        case "set":
        case "SET": target.invokeSet(exchange); return null;
        case "setbit":
        case "SETBIT": target.invokeSetbit(exchange); return null;
        case "setex":
        case "SETEX": target.invokeSetex(exchange); return null;
        case "setnx":
        case "SETNX": return target.invokeSetnx(exchange);
        case "setrange":
        case "SETRANGE": target.invokeSetrange(exchange); return null;
        case "sinter":
        case "SINTER": return target.invokeSinter(exchange);
        case "sinterstore":
        case "SINTERSTORE": target.invokeSinterstore(exchange); return null;
        case "sismember":
        case "SISMEMBER": return target.invokeSismember(exchange);
        case "smembers":
        case "SMEMBERS": return target.invokeSmembers(exchange);
        case "smove":
        case "SMOVE": return target.invokeSmove(exchange);
        case "sort":
        case "SORT": return target.invokeSort(exchange);
        case "spop":
        case "SPOP": return target.invokeSpop(exchange);
        case "srandmember":
        case "SRANDMEMBER": return target.invokeSrandmember(exchange);
        case "srem":
        case "SREM": return target.invokeSrem(exchange);
        case "strlen":
        case "STRLEN": return target.invokeStrlen(exchange);
        case "sunion":
        case "SUNION": return target.invokeSunion(exchange);
        case "sunionstore":
        case "SUNIONSTORE": target.invokeSunionstore(exchange); return null;
        case "ttl":
        case "TTL": return target.invokeTtl(exchange);
        case "type":
        case "TYPE": return target.invokeType(exchange);
        case "unwatch":
        case "UNWATCH": target.invokeUnwatch(exchange); return null;
        case "watch":
        case "WATCH": target.invokeWatch(exchange); return null;
        case "zadd":
        case "ZADD": return target.invokeZadd(exchange);
        case "zcard":
        case "ZCARD": return target.invokeZcard(exchange);
        case "zcount":
        case "ZCOUNT": return target.invokeZcount(exchange);
        case "zincrby":
        case "ZINCRBY": return target.invokeZincrby(exchange);
        case "zinterstore":
        case "ZINTERSTORE": target.invokeZinterstore(exchange); return null;
        case "zrange":
        case "ZRANGE": return target.invokeZrange(exchange);
        case "zrangebyscore":
        case "ZRANGEBYSCORE": return target.invokeZrangebyscore(exchange);
        case "zrank":
        case "ZRANK": return target.invokeZrank(exchange);
        case "zrem":
        case "ZREM": return target.invokeZrem(exchange);
        case "zremrangebyrank":
        case "ZREMRANGEBYRANK": target.invokeZremrangebyrank(exchange); return null;
        case "zremrangebyscore":
        case "ZREMRANGEBYSCORE": target.invokeZremrangebyscore(exchange); return null;
        case "zrevrange":
        case "ZREVRANGE": return target.invokeZrevrange(exchange);
        case "zrevrangebyscore":
        case "ZREVRANGEBYSCORE": return target.invokeZrevrangebyscore(exchange);
        case "zrevrank":
        case "ZREVRANK": return target.invokeZrevrank(exchange);
        case "zunionstore":
        case "ZUNIONSTORE": target.invokeZunionstore(exchange); return null;
        default: return null;
        }
    }

}

