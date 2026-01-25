package lost_and_found.app.items.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import lost_and_found.app.items.domain.ItemPost;

public final class ItemSpecifications {

    private ItemSpecifications() {}

    public static ItemSearchSpecification fromCriteria(ItemSearchCriteria criteria) {
        return new ItemSearchSpecification(criteria);
    }

    public static final class ItemSearchSpecification implements Specification<ItemPost> {
        private final ItemSearchCriteria criteria;

        private ItemSearchSpecification(ItemSearchCriteria criteria) {
            this.criteria = criteria;
        }

        public boolean matches(ItemPost item) {
            if (criteria.status() != null && item.getStatus() != criteria.status()) {
                return false;
            }
            if (criteria.category() != null) {
                String itemCategory = item.getCategory();
                if (itemCategory == null || !itemCategory.equalsIgnoreCase(criteria.category())) {
                    return false;
                }
            }
            if (criteria.userId() != null) {
                if (item.getUser() == null || !criteria.userId().equals(item.getUser().getId())) {
                    return false;
                }
            }
            if (criteria.postType() != null && item.getPostType() != criteria.postType()) {
                return false;
            }
            return true;
        }

        @Override
        public Predicate toPredicate(
            jakarta.persistence.criteria.Root<ItemPost> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder
        ) {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.category() != null) {
                predicates.add(
                    criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        criteria.category().toLowerCase()
                    )
                );
            }
            if (criteria.userId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), criteria.userId()));
            }
            if (criteria.postType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("postType"), criteria.postType()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        }
    }
}
