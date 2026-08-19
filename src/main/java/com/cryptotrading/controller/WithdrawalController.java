package com.cryptotrading.controller;

import com.cryptotrading.dto.WithdrawalProcessRequest;
import com.cryptotrading.dto.WithdrawalRequest;
import com.cryptotrading.dto.WithdrawalResponse;
import com.cryptotrading.model.User;
import com.cryptotrading.model.Withdrawal;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/withdrawal")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(
            @RequestHeader("Authorization") String jwt,
            @Valid @RequestBody WithdrawalRequest request) {

        User user = userService.findUserProfileByJwt(jwt);
        Withdrawal withdrawal = withdrawalService.requestWithdrawal(
                request.getAmount(),
                user
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(WithdrawalResponse.from(withdrawal));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/process")
    public ResponseEntity<WithdrawalResponse> processWithdrawal(
            @PathVariable Long id,
            @Valid @RequestBody WithdrawalProcessRequest request,
            @RequestHeader("Authorization") String jwt)  {

       userService.findUserProfileByJwt(jwt);

        Withdrawal withdrawal = withdrawalService.processWithdrawal(
                id,
                request.getAccept()
        );

        return ResponseEntity.ok(
                WithdrawalResponse.from(withdrawal)
        );
    }

    @GetMapping
    public ResponseEntity<List<WithdrawalResponse>> getWithdrawalHistory(
          @RequestHeader("Authorization") String jwt){

        User user = userService.findUserProfileByJwt(jwt);

        List<WithdrawalResponse> responses =
                withdrawalService
                        .getUsersWithdrawalHistory(user)
                        .stream()
                        .map(WithdrawalResponse::from)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<List<WithdrawalResponse>> getAllWithdrawalRequests(
            @RequestHeader("Authorization") String jwt) {

        userService.findUserProfileByJwt(jwt);

        List<WithdrawalResponse> response =
                withdrawalService
                        .getAllWithdrawalRequest()
                        .stream()
                        .map(WithdrawalResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
}
