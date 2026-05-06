package com.app.admin.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOverviewResponse {
    private long pendingProviderVerifications;
    private long approvedProviders;
    private long rejectedProviders;
    private long reportedReviews;
}
