package com.ecommerce.catalog.application.service;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;
import com.ecommerce.catalog.application.dto.ChangeProductPriceCommand;
import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.application.dto.CreateProductCommand;
import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.ProductQueryResult;
import com.ecommerce.catalog.application.dto.ProductSummary;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;
import com.ecommerce.catalog.application.dto.UpdateProductCommand;
import com.ecommerce.catalog.application.port.in.ActivateProductUseCase;
import com.ecommerce.catalog.application.port.in.AssignCategoryToProductUseCase;
import com.ecommerce.catalog.application.port.in.ChangeProductPriceUseCase;
import com.ecommerce.catalog.application.port.in.CreateCategoryUseCase;
import com.ecommerce.catalog.application.port.in.CreateProductUseCase;
import com.ecommerce.catalog.application.port.in.GetCategoriesUseCase;
import com.ecommerce.catalog.application.port.in.GetProductUseCase;
import com.ecommerce.catalog.application.port.in.RemoveCategoryFromProductUseCase;
import com.ecommerce.catalog.application.port.in.RetireProductUseCase;
import com.ecommerce.catalog.application.port.in.SearchProductsUseCase;
import com.ecommerce.catalog.application.port.in.UpdateProductUseCase;
import com.ecommerce.catalog.application.port.out.EventPublisher;
import com.ecommerce.catalog.domain.event.DomainEvent;
import com.ecommerce.catalog.domain.exception.CategoryNotFoundException;
import com.ecommerce.catalog.domain.exception.DuplicateCategoryException;
import com.ecommerce.catalog.domain.exception.ProductNotFoundException;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.repository.CategoryRepository;
import com.ecommerce.catalog.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CatalogApplicationService implements CreateProductUseCase, UpdateProductUseCase,
        ChangeProductPriceUseCase, ActivateProductUseCase, RetireProductUseCase, GetProductUseCase,
        SearchProductsUseCase, CreateCategoryUseCase, GetCategoriesUseCase,
        AssignCategoryToProductUseCase, RemoveCategoryFromProductUseCase {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EventPublisher eventPublisher;

    public CatalogApplicationService(ProductRepository productRepository,
                                     CategoryRepository categoryRepository,
                                     EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ProductId createProduct(CreateProductCommand command) {
        Product product = Product.create(
                ProductId.newId(),
                command.name(),
                command.description(),
                new Money(command.price(), command.currency()),
                command.companyId());

        productRepository.save(product);
        publishEvents(product);
        return product.id();
    }

    @Override
    public void updateProduct(CompanyId companyId, ProductId productId, UpdateProductCommand command) {
        Product product = loadProduct(companyId, productId);
        product.update(command.name(), command.description());
        productRepository.save(product);
        publishEvents(product);
    }

    @Override
    public void changeProductPrice(CompanyId companyId, ProductId productId, ChangeProductPriceCommand command) {
        Product product = loadProduct(companyId, productId);
        product.changePrice(new Money(command.price(), command.currency()));
        productRepository.save(product);
        publishEvents(product);
    }

    @Override
    public void activateProduct(CompanyId companyId, ProductId productId) {
        Product product = loadProduct(companyId, productId);
        product.activate();
        productRepository.save(product);
        publishEvents(product);
    }

    @Override
    public void retireProduct(CompanyId companyId, ProductId productId) {
        Product product = loadProduct(companyId, productId);
        product.retire();
        productRepository.save(product);
        publishEvents(product);
    }

    @Override
    public ProductQueryResult getProduct(CompanyId companyId, ProductId productId) {
        return ProductQueryResult.from(loadProduct(companyId, productId));
    }

    @Override
    public ProductPageResult search(CompanyId companyId, SearchProductsQuery query) {
        List<Product> matches = productRepository.findAllByCompanyId(companyId).stream()
                .filter(product -> matchesKeyword(product, query.keyword()))
                .filter(product -> query.categoryId() == null || product.categories().contains(query.categoryId()))
                .filter(product -> query.status() == null || product.status() == query.status())
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();

        int from = Math.min(query.page() * query.size(), matches.size());
        int to = Math.min(from + query.size(), matches.size());
        List<ProductSummary> pageItems = matches.subList(from, to).stream()
                .map(ProductSummary::from)
                .toList();

        return ProductPageResult.of(pageItems, query.page(), query.size(), matches.size());
    }

    @Override
    public CategoryId createCategory(CreateCategoryCommand command) {
        if (categoryRepository.existsByName(command.companyId(), command.name())) {
            throw new DuplicateCategoryException(command.name());
        }
        Category category = Category.create(CategoryId.newId(), command.name(), command.parentId(), command.companyId());
        categoryRepository.save(category);
        return category.id();
    }

    @Override
    public List<CategoryQueryResult> getCategories(CompanyId companyId) {
        return categoryRepository.findAllByCompanyId(companyId).stream()
                .map(CategoryQueryResult::from)
                .toList();
    }

    @Override
    public void assignCategory(CompanyId companyId, ProductId productId, CategoryId categoryId) {
        Product product = loadProduct(companyId, productId);
        if (categoryRepository.findById(companyId, categoryId).isEmpty()) {
            throw new CategoryNotFoundException(categoryId);
        }
        product.assignCategory(categoryId);
        productRepository.save(product);
    }

    @Override
    public void removeCategory(CompanyId companyId, ProductId productId, CategoryId categoryId) {
        Product product = loadProduct(companyId, productId);
        product.removeCategory(categoryId);
        productRepository.save(product);
    }

    private boolean matchesKeyword(Product product, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase(Locale.ROOT);
        return product.name().toLowerCase(Locale.ROOT).contains(lower)
                || (product.description() != null && product.description().toLowerCase(Locale.ROOT).contains(lower));
    }

    private Product loadProduct(CompanyId companyId, ProductId productId) {
        return productRepository.findById(companyId, productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void publishEvents(Product product) {
        for (DomainEvent event : product.pullDomainEvents()) {
            eventPublisher.publish(event);
        }
    }
}
