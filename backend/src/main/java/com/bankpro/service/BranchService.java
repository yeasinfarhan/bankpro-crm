package com.bankpro.service;

import com.bankpro.dto.BranchResponse;
import com.bankpro.entity.Role;
import com.bankpro.mapper.UserMapper;
import com.bankpro.repository.BranchRepository;
import com.bankpro.security.AuthenticatedUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository branches;

    public BranchService(BranchRepository branches) {
        this.branches = branches;
    }

    /** ADMIN gets every branch (for branch filters); everyone else only sees their own branch. */
    public List<BranchResponse> listVisible(AuthenticatedUser caller) {
        if (caller.role() == Role.ADMIN) {
            return branches.findAll(Sort.by("name")).stream().map(UserMapper::toResponse).toList();
        }
        return branches.findById(caller.branchId()).stream().map(UserMapper::toResponse).toList();
    }
}
