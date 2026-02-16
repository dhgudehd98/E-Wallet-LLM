package com.sh.ewalletllm.chathistory.service;


import com.sh.ewalletllm.redis.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class UserChatHistoryService {

    private final ReactiveRedisTemplate<String, ChatMessageDto> redisTemplate;
    private static final int MAX_HISTORY = 10;

    private String key(Long memberId){
        return "chat : " + memberId;
    }

    // 메세지 내역 저장
    public Mono<Long> saveMessage(Long memberId, ChatMessageDto messageDto) {
        return redisTemplate.opsForList()
                .rightPush(key(memberId), messageDto)
                .flatMap(size -> {
                   if(size > MAX_HISTORY) return trimHistory(memberId).thenReturn(size);
                   return Mono.just(size);
                });
    }

    // 최근 메세지 내역 가져오기 -> 메세지 내역이 여러개일수도 있으므로 Flux로
    public Flux<ChatMessageDto> getRecentHistory(Long memberId) {
        return redisTemplate.opsForList()
                .range(key(memberId), -MAX_HISTORY, -1)
                .subscribeOn(Schedulers.boundedElastic());
    }

    // Redis 안에는 Message 내역 10개만 저장하기
    private Mono<Boolean> trimHistory(Long memberId) {
        return redisTemplate.opsForList()
                .trim(key(memberId), -MAX_HISTORY, -1);
    }


}