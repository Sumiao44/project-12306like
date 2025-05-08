-- 原子性获取给定key，若key存在返回其值，若key不存在则设置key并返回null
local key = KEYS[1]
local value = ARGV[1]
local expire_time_ms = ARGV[2]

-- 检查 key 是否存在
local exists = redis.call('EXISTS', key)
if exists == 0 then
    -- key 不存在，设置值并设置过期时间
    redis.call('SET', key, value, 'PX', expire_time_ms)
    return nil  -- 返回 null
else
    -- key 存在，返回当前值
    return redis.call('GET', key)
end
