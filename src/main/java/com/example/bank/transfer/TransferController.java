package com.example.bank.transfer;

import com.example.bank.account.dto.TransferRequest;
import com.example.bank.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal User user) {
        
        transferService.transfer(request, user);
        return ResponseEntity.ok().build();
    }
}
