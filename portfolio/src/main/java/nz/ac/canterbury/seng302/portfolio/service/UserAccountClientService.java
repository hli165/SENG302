package nz.ac.canterbury.seng302.portfolio.service;

import net.devh.boot.grpc.client.inject.GrpcClient;
import nz.ac.canterbury.seng302.shared.identityprovider.*;
import org.springframework.stereotype.Service;

@Service
public class UserAccountClientService {

    @GrpcClient(value = "identity-provider-grpc-server")
    private UserAccountServiceGrpc.UserAccountServiceBlockingStub userAccountStub;

    /**
     * Returns the user id from the given AuthState.
     * @param principal AutState to extract the user id from
     * @return user id from the given AuthState
     */
    public Integer getUserIDFromAuthState(AuthState principal) {
        return Integer.valueOf(principal.getClaimsList().stream()
                .filter(claim -> claim.getType().equals("nameid"))
                .findFirst()
                .map(ClaimDTO::getValue)
                .orElse("-100"));
    }

    /**
     * Returns the user role from the given AuthState.
     * @param principal AutState to extract the user role from
     * @return user role from the given AuthState
     */
    public String getRoleFromAuthState(AuthState principal) {
        return principal.getClaimsList().stream()
                .filter(claim -> claim.getType().equals("role"))
                .findFirst()
                .map(ClaimDTO::getValue)
                .orElse("NOT FOUND");
    }


    public PaginatedUsersResponse getAllUsers() {
        GetPaginatedUsersRequest response = GetPaginatedUsersRequest.newBuilder()
                .build();
        return userAccountStub.getPaginatedUsers(response);
    }
}