# build the react application
FROM node:24-alpine AS build

WORKDIR /app

# copy dependency files first for Docker caching
COPY package.json package-lock.json ./

RUN npm ci

# copy frontend source files
COPY . .

# build the production React application
RUN npm run build


# serve the built application with Nginx
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]